package crystal.react.hooks

import cats.Monoid
import cats.effect.Async
import cats.effect.Deferred
import cats.effect.Ref
import cats.effect.syntax.all._
import cats.syntax.all._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

import scala.concurrent.duration.FiniteDuration

class UseSingleEffect[F[_]](
  latch:      Ref[F, Option[Deferred[F, UnitFiber[F]]]],
  debounce:   Option[FiniteDuration]
)(implicit F: Async[F], monoid: Monoid[F[Unit]]) {
  private val debounceEffect: F[Unit] = debounce.map(F.sleep).orEmpty

  private def switchTo(effect: F[Unit]): F[Unit] =
    Deferred[F, UnitFiber[F]] >>= (newLatch =>
      latch
        .modify(oldLatch =>
          (newLatch.some,
           for {
             // Cleanup latch after effect + debounce, so that we don't run debounce again next time.
             newFiber <- (oldLatch.map(_.get.flatMap(_.cancel >> debounceEffect)).orEmpty >>
                           effect >> debounceEffect >> latch.set(none)).start
             _        <- newLatch.complete(newFiber)
           } yield ()
          )
        )
        .flatten
        .uncancelable
    )

  val cancel: F[Unit] = switchTo(F.unit)

  // There's no need to clean up the fiber reference once the effect completes.
  // Worst case scenario, cancel will be called on it, which will do nothing.
  def submit(effect: F[Unit]): F[Unit] = switchTo(effect)
}

object UseSingleEffect {

  val hook = CustomHook[Option[FiniteDuration]]
    .useMemoBy(_ => ())(debounce =>
      _ =>
        new UseSingleEffect(
          Ref.unsafe[DefaultA, Option[Deferred[DefaultA, UnitFiber[DefaultA]]]](none),
          debounce
        )
    )
    .useEffectBy((_, singleEffect) => Callback(singleEffect.cancel)) // Cleanup on unmount
    .buildReturning((_, singleEffect) => singleEffect)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Provides a context in which to run a single effect at a time. When a new effect is
        * submitted, the previous one is canceled. Also cancels the effect on unmount.
        *
        * A submitted effect can be explicitly canceled too.
        */
      final def useSingleEffect(debounce: FiniteDuration)(implicit
        step:                             Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        useSingleEffectBy(_ => debounce)

      /** Provides a context in which to run a single effect at a time. When a new effect is
        * submitted, the previous one is canceled. Also cancels the effect on unmount.
        *
        * A submitted effect can be explicitly canceled too.
        */
      final def useSingleEffect(implicit
        step: Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        api.customBy(_ => hook(none))

      /** Provides a context in which to run a single effect at a time. When a new effect is
        * submitted, the previous one is canceled. Also cancels the effect on unmount.
        *
        * A submitted effect can be explicitly canceled too.
        */
      final def useSingleEffectBy(debounce: Ctx => FiniteDuration)(implicit
        step:                               Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        api.customBy(ctx => hook(debounce(ctx).some))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Provides a context in which to run a single effect at a time. When a new effect is
        * submitted, the previous one is canceled. Also cancels the effect on unmount.
        *
        * A submitted effect can be explicitly canceled too.
        *
        * `debounce` can specify a minimum `Duration` between invocations.
        */
      def useSingleEffectBy(debounce: CtxFn[FiniteDuration])(implicit
        step:                         Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        useSingleEffectBy(step.squash(debounce)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtSingleEffect1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtSingleEffect2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
