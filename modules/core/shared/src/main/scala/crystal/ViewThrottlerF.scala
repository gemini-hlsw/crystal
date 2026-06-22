// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.effect.Ref
import cats.effect.Temporal
import cats.effect.syntax.all.*
import cats.syntax.all.*
import cats.~>
import monocle.Focus
import monocle.Lens

import scala.concurrent.duration.FiniteDuration

/**
 * Given a `ViewF`, creates two `ViewF`s over the same value:
 *   - A `throttledView`, which can be paused. While paused, it accumulates updates and applies them
 *     all at once upon timeout. The callback is called only once and only to the function provided
 *     in the last update during the pause. If unpaused, it acts as a normal `ViewF`.
 *   - A `throttlerView`, which will pause the `throttledView` whenever it is modified.
 * This is particularly useful for values that can be both updated from a UI and from a server. The
 * `throttlerView` should be used in the UI, while the `throttledView` should be used for the server
 * updates. This way, the server updates will pause whenever the user changes a value. If the server
 * sends updates for every changed value, the throttling will avoid the UI from glitching between
 * old and new values when the UI is updated quickly.
 * @typeparam
 *   F The sync effect. Your `ViewF` should be in this effect.
 * @typeparam
 *   G The async effect. This will be used for concurrency.
 */
final class ViewThrottlerF[F[_], G[_], A] private (
  state:         Ref[G, ViewThrottlerF.State[G, A]],
  timeout:       FiniteDuration,
  syncToAsync:   F ~> G,
  dispatchAsync: G[Unit] => F[Unit]
)(using G: Temporal[G]) {
  import ViewThrottlerF.State

  private def throttle: F[Unit] =
    dispatchAsync:
      G.monotonic.flatMap(now => state.update(State.waitUntil[G, A].replace(now + timeout)))

  private def throttlerView(view: ViewF[F, A]): ViewF[F, A] =
    view.withOnMod(_ => throttle)

  private def attemptSet(modCB: (A => A, (A, A) => G[Unit]) => G[Unit]): G[Unit] =
    G.monotonic.flatMap: now =>
      state
        .modify: s =>
          if (s.waitUntil > now)
            (s, (G.sleep(s.waitUntil - now) >> attemptSet(modCB)).start.void)
          else
            (State.nextUpdate[G, A].replace(none)(s),
             s.nextUpdate.map((mod, cb) => modCB(mod, cb)).getOrElse(G.unit)
            )
        .flatten

  private def throttledView(view: ViewF[F, A]): ViewF[G, A] =
    new ViewF[G, A](
      get = view.get,
      modCB = (mod, cb) =>
        state.update(State.nextUpdate[G, A].modify {
          case None            => (mod, cb).some
          case Some(oldMod, _) => (oldMod.andThen(mod), cb).some // We only keep last CB
        }) >>
          attemptSet: (newMod, newCB) =>
            syncToAsync(view.modCB(newMod, (oldA, newA) => dispatchAsync(newCB(oldA, newA))))
    )

  def throttle(view: ViewF[F, A]): ThrottlingViewF[F, G, A] =
    ThrottlingViewF(throttlerView(view), throttledView(view))
}

object ViewThrottlerF {
  // The type of ViewF.modCB's parameters, to avoid repeating everywhere.
  private type ModCBType[F[_], A] = (A => A, (A, A) => F[Unit])

  // `waitUntil` (pause deadline) and `nextUpdate` (last accumulated update) are kept in a single
  // `Ref` so that, in `attemptSet`, deciding whether the pause has elapsed and taking the pending
  // update happen as one atomic transition.
  private case class State[G[_], A](waitUntil: FiniteDuration, nextUpdate: Option[ModCBType[G, A]])
  private object State:
    def waitUntil[G[_], A]: Lens[State[G, A], FiniteDuration]           =
      Focus[State[G, A]](_.waitUntil)
    def nextUpdate[G[_], A]: Lens[State[G, A], Option[ModCBType[G, A]]] =
      Focus[State[G, A]](_.nextUpdate)

  def apply[F[_], G[_], A](
    timeout:       FiniteDuration,
    syncToAsync:   F ~> G,
    dispatchAsync: G[Unit] => F[Unit]
  )(using G: Temporal[G]): G[ViewThrottlerF[F, G, A]] =
    G.monotonic
      .flatMap(now => G.ref(State[G, A](now, none)))
      .map(new ViewThrottlerF(_, timeout, syncToAsync, dispatchAsync))
}
