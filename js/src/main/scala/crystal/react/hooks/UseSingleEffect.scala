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

class UseSingleEffect[F[_]](
  latch:      Ref[F, Option[Deferred[F, UseSingleEffectLatch[F]]]]
)(implicit F: Async[F], monoid: Monoid[F[Unit]]) {

  private def switchTo(effect: F[Unit]): F[Unit] =
    Deferred[F, UseSingleEffectLatch[F]] >>= (newLatch =>
      latch
        .modify(oldLatch =>
          (newLatch.some,
           for {
             _        <- oldLatch.map(_.get.flatMap(_.cancel)).orEmpty
             newFiber <- effect.start
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

  val hook = CustomHook[Unit]
    .useMemoBy(_ => ())(_ =>
      _ =>
        new UseSingleEffect(
          Ref.unsafe[DefaultA, Option[Deferred[DefaultA, UseSingleEffectLatch[DefaultA]]]](none)
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
      final def useSingleEffect(implicit
        step: Step
      ): step.Next[Reusable[UseSingleEffect[DefaultA]]] =
        api.customBy(_ => hook)
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {}
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
