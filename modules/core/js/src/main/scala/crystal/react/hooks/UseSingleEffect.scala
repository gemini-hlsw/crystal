// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.Monoid
import cats.Parallel
import cats.effect.Async
import cats.effect.Deferred
import cats.effect.Ref
import cats.effect.syntax.all.given
import cats.syntax.all.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

class UseSingleEffect[F[_]](
  latch:   Ref[F, Option[Deferred[F, UnitFiber[F]]]], // latch released as effect starts, holds fiber
  cleanup: Ref[F, Option[F[Unit]]]                    // cleanup of the currently running effect
)(using F: Async[F], parF: Parallel[F], monoid: Monoid[F[Unit]]):
  private def endOldEffect(oldLatch: Deferred[F, UnitFiber[F]]): F[Unit] =
    // 1) We ensure the effect of the last call has started by waiting for the latch.
    oldLatch.get.flatMap: oldFiber =>
      // 2a) If the effect is still running, we cancel it. Noop if it has already completed; or
      // 2b) If the effect has completed, we run its cleanup effect (will be none if it's still running).
      val cleanupEffect: F[Unit] = cleanup.getAndSet(none).flatMap(_.orEmpty)
      (oldFiber.cancel, cleanupEffect).parTupled.void

  private def startNewEffect(effect: F[F[Unit]], newLatch: Deferred[F, UnitFiber[F]]): F[Unit] =
    effect
      .flatMap(f => cleanup.set(f.some)) // When effect completes, store cleanup effect.
      .start
      .flatMap: newFiber =>
        newLatch.complete(newFiber).void // Store the running fiber, releasing the latch.

  private def switchTo(effect: F[F[Unit]]): F[Unit] =
    Deferred[F, UnitFiber[F]] >>= (newLatch =>
      latch
        .modify: oldLatch =>
          (
            newLatch.some,                        // Replace current latch with a new one.
            oldLatch.map(endOldEffect).orEmpty >> // Cancel and cleanup old effect, if any.
              startNewEffect(effect, newLatch)    // Start new effect.
          )
        .flatten
        .uncancelable // We can't cancel before new latch is set, otherwise we deadlock.
    )

  val cancel: F[Unit] = switchTo(F.unit.pure[F])

  // There's no need to clean up the fiber reference once the effect completes.
  // Worst case scenario, cancel will be called on it, which will do nothing.
  def submit[G](effect: G)(using EffectWithCleanup[G, F]) =
    switchTo(effect.normalize)

object UseSingleEffect:
  /**
   * Provides a context in which to run a single effect at a time. When a new effect is submitted,
   * the previous one is canceled. Also cancels the effect on unmount.
   *
   * A submitted effect can be explicitly canceled too.
   */
  final def useSingleEffect: HookResult[Reusable[UseSingleEffect[DefaultA]]] =
    for
      singleEffect <-
        useMemo(()): _ =>
          new UseSingleEffect(
            Ref.unsafe[DefaultA, Option[Deferred[DefaultA, UnitFiber[DefaultA]]]](none),
            Ref.unsafe[DefaultA, Option[DefaultA[Unit]]](none)
          )
      _            <-
        useEffectOnMount(CallbackTo(singleEffect.cancel))
    yield singleEffect

  // *** The rest is to support builder-style hooks *** //

  private val hook: CustomHook[Unit, Reusable[UseSingleEffect[DefaultA]]] =
    CustomHook.fromHookResult(useSingleEffect)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Provides a context in which to run a single effect at a time. When a new effect is
       * submitted, the previous one is canceled. Also cancels the effect on unmount.
       *
       * A submitted effect can be explicitly canceled too.
       */
      final def useSingleEffect(using step: Step): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        api.customBy(_ => hook)
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtSingleEffect1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)
  }

  object syntax extends HooksApiExt
