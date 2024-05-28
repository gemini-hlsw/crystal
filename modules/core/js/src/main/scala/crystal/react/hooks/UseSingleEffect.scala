// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.Monoid
import cats.effect.Async
import cats.effect.Deferred
import cats.effect.Ref
import cats.effect.syntax.all.given
import cats.syntax.all.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

import scala.concurrent.duration.FiniteDuration
import cats.Applicative
import scala.annotation.targetName

trait EffectWithCleanup[G, F[_]]:
  def resolve(f: G): F[F[Unit]]

given effectWithNoCleanup: EffectWithCleanup[DefaultA[DefaultA[Unit]], DefaultA] with
  def resolve(f: DefaultA[DefaultA[Unit]]): DefaultA[DefaultA[Unit]] = f

given effectWithCleanup: EffectWithCleanup[DefaultA[Unit], DefaultA] with
  def resolve(f: DefaultA[Unit]): DefaultA[DefaultA[Unit]] = f.as(Applicative[DefaultA].unit)

class UseSingleEffect[F[_]](
  latch:    Ref[F, Option[Deferred[F, UnitFiber[F]]]],
  cleanup:  Ref[F, Option[F[Unit]]],
  debounce: Option[FiniteDuration]
)(using F: Async[F], monoid: Monoid[F[Unit]]) {
  // given effectWithNoCleanup: EffectWithCleanup[F[F[Unit]], F] with
  //   def resolve(f: F[F[Unit]]): F[F[Unit]] = f

  // given effectWithCleanup: EffectWithCleanup[F[Unit], F] with
  //   def resolve(f: F[Unit]): F[F[Unit]] = f.as(F.unit)

  private val debounceEffect: F[Unit] = debounce.map(F.sleep).orEmpty

  private def switchTo(effect: F[F[Unit]]): F[Unit] =
    Deferred[F, UnitFiber[F]] >>= (newLatch =>
      latch
        .modify: oldLatch =>
          (
            newLatch.some,
            (
              oldLatch
                .map:
                  _.get.flatMap(_.cancel >> debounceEffect) >>
                    cleanup.getAndSet(none).flatMap(_.orEmpty)
                .orEmpty >>
                effect.flatMap(f => cleanup.set(f.some)) >>
                debounceEffect >>
                // Discard latch after effect + debounce, so that we don't run debounce again next time.
                latch.set(none)
            ).start
              .flatMap: newFiber =>
                newLatch.complete(newFiber).void
          )
        .flatten
        .uncancelable
    )

  val cancel: F[Unit] = switchTo(F.unit.pure[F])

  // There's no need to clean up the fiber reference once the effect completes.
  // Worst case scenario, cancel will be called on it, which will do nothing.
  def submit(effect: F[F[Unit]]): F[Unit] = switchTo(effect)

  @targetName("submitNoCleanup")
  def submit(effect: F[Unit]): F[Unit] = switchTo(effect.as(F.unit))

  // def submit[G](effect: G)(using resolver: EffectWithCleanup[G, F]) = switchTo(
  //   resolver.resolve(effect)
  // )
}

object UseSingleEffect {
  val hook = CustomHook[Option[FiniteDuration]]
    .useMemoBy(_ => ())(debounce =>
      _ =>
        new UseSingleEffect(
          Ref.unsafe[DefaultA, Option[Deferred[DefaultA, UnitFiber[DefaultA]]]](none),
          Ref.unsafe[DefaultA, Option[DefaultA[Unit]]](none),
          debounce
        )
    )
    .useEffectBy((_, singleEffect) => Callback(singleEffect.cancel)) // Cleanup on unmount
    .buildReturning((_, singleEffect) => singleEffect)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Provides a context in which to run a single effect at a time. When a new effect is
       * submitted, the previous one is canceled. Also cancels the effect on unmount.
       *
       * A submitted effect can be explicitly canceled too.
       */
      final def useSingleEffect(debounce: FiniteDuration)(using
        step: Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        useSingleEffectBy(_ => debounce)

      /**
       * Provides a context in which to run a single effect at a time. When a new effect is
       * submitted, the previous one is canceled. Also cancels the effect on unmount.
       *
       * A submitted effect can be explicitly canceled too.
       */
      final def useSingleEffect(using
        step: Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        api.customBy(_ => hook(none))

      /**
       * Provides a context in which to run a single effect at a time. When a new effect is
       * submitted, the previous one is canceled. Also cancels the effect on unmount.
       *
       * A submitted effect can be explicitly canceled too.
       */
      final def useSingleEffectBy(debounce: Ctx => FiniteDuration)(using
        step: Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        api.customBy(ctx => hook(debounce(ctx).some))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Provides a context in which to run a single effect at a time. When a new effect is
       * submitted, the previous one is canceled. Also cancels the effect on unmount.
       *
       * A submitted effect can be explicitly canceled too.
       *
       * `debounce` can specify a minimum `Duration` between invocations.
       */
      def useSingleEffectBy(debounce: CtxFn[FiniteDuration])(using
        step: Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        useSingleEffectBy(step.squash(debounce)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtSingleEffect1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtSingleEffect2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
