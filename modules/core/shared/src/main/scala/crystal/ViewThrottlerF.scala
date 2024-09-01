// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Applicative
import cats.effect.Ref
import cats.effect.Temporal
import cats.syntax.all.*
import cats.~>

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
  waitUntil:     Ref[G, FiniteDuration],
  nextUpdate:    Ref[G, (A => A, (A, A) => G[Unit])],
  timeout:       FiniteDuration,
  syncToAsync:   F ~> G,
  dispatchAsync: G[Unit] => F[Unit]
)(using G: Temporal[G]) {

  private def curb: F[Unit] =
    dispatchAsync:
      G.monotonic.flatMap(now => waitUntil.set(now + timeout))

  private def throttlerView(view: ViewF[F, A]): ViewF[F, A] =
    view.withOnMod(_ => curb) // todo try to curb before modding?? maybe not necessary

  private def attemptSet(modCB: (A => A, (A, A) => G[Unit]) => G[Unit]): G[Unit] =
    (waitUntil.get, G.monotonic).flatMapN: (waitUntil, now) =>
      if (waitUntil > now)
        G.sleep(waitUntil - now) >> attemptSet(modCB)
      else
        nextUpdate.flatModify((mod, cb) => (ViewThrottlerF.initialValue[G, A], modCB(mod, cb)))

  private def throttledView(view: ViewF[F, A]): ViewF[G, A] =
    new ViewF[G, A](
      get = view.get,
      modCB = (mod, cb) => // We only keep last CB
        nextUpdate.update((oldMod, _) => (oldMod.andThen(mod), cb)) >>
          attemptSet: (newMod, newCB) =>
            syncToAsync(view.modCB(newMod, (oldA, newA) => dispatchAsync(newCB(oldA, newA))))
    )

  def throttle(view: ViewF[F, A]): ThrottlingViewF[F, G, A] =
    ThrottlingViewF(throttlerView(view), throttledView(view))
}

object ViewThrottlerF {
  private inline def initialValue[F[_], A](using F: Applicative[F]): (A => A, (A, A) => F[Unit]) =
    (identity, (_, _) => F.unit)

  def apply[F[_], G[_], A](
    timeout:       FiniteDuration,
    syncToAsync:   F ~> G,
    dispatchAsync: G[Unit] => F[Unit]
  )(using G: Temporal[G]): G[ViewThrottlerF[F, G, A]] =
    (G.monotonic.flatMap(G.ref(_)), G.ref(initialValue[G, A]))
      .mapN(new ViewThrottlerF(_, _, timeout, syncToAsync, dispatchAsync))
}
