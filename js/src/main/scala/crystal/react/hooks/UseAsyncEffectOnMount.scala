package crystal.react.hooks

import cats.syntax.all._
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

// TODO PR to scalajs-react changing implicit not found message
object UseAsyncEffectOnMount {

  val hook = CustomHook[DefaultA[DefaultA[Unit]]]
    .useRef(none[DefaultA[Unit]])
    .useEffectOnMountBy((effect, cleanup) =>
      effect
        .flatMap(f => cleanup.setAsync(f.some))
        .runAsyncAndForget
        .as(cleanup.get.flatMap(_.foldMap(_.runAsyncAndForget)))
    )
    .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Simulates `useEffectOnMount` with cleanup callback for async effect. To declare an async
        * effect without a cleanup callback, just use the regular `useEffectOnMount` hook.
        */
      final def useAsyncEffectOnMount[A](effect: DefaultA[DefaultA[Unit]])(implicit
        step:                                    Step
      ): step.Self =
        useAsyncEffectOnMountBy(_ => effect)

      /** Simulates `useEffectOnMount` with cleanup callback for async effect. To declare an async
        * effect without a cleanup callback, just use the regular `useEffectOnMount` hook.
        */
      final def useAsyncEffectOnMountBy[A](effect: Ctx => DefaultA[DefaultA[Unit]])(implicit
        step:                                      Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook
          hookInstance(effect(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Simulates `useEffectOnMount` with cleanup callback for async effect. To declare an async
        * effect without a cleanup callback, just use the regular `useEffectOnMount` hook.
        */
      def useAsyncEffectOnMountBy[A](effect: CtxFn[DefaultA[DefaultA[Unit]]])(implicit
        step:                                Step
      ): step.Self =
        useAsyncEffectOnMountBy(step.squash(effect)(_))
    }

  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtAsyncEffectOnMount1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtAsyncEffectOnMount2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                            CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
