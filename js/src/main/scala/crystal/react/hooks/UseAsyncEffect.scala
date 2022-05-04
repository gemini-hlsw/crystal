package crystal.react.hooks

import cats.syntax.all._
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

// TODO PR to scalajs-react changing implicit not found message
object UseAsyncEffect {

  val hook = CustomHook[DefaultA[DefaultA[Unit]]]
    .useRef(none[DefaultA[Unit]])
    .useEffectBy((effect, cleanupEffect) =>
      effect
        .flatMap(f => cleanupEffect.setAsync(f.some))
        .runAsyncAndForget
        .as(cleanupEffect.get.flatMap(_.foldMap(_.runAsyncAndForget)))
    )
    .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      final def useAsyncEffect[A](effect: DefaultA[DefaultA[Unit]])(implicit
        step:                             Step
      ): step.Self =
        useAsyncEffectBy(_ => effect)

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      final def useAsyncEffectBy[A](effect: Ctx => DefaultA[DefaultA[Unit]])(implicit
        step:                               Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook
          hookInstance(effect(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      def useAsyncEffectBy[A](effect: CtxFn[DefaultA[DefaultA[Unit]]])(implicit
        step:                         Step
      ): step.Self =
        useAsyncEffectBy(step.squash(effect)(_))
    }

  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtAsyncEffect1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtAsyncEffect2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
