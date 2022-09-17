package crystal.react.hooks

import cats.syntax.all._
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseAsyncEffect {
  def hook[D: Reusability] = CustomHook[WithDeps[D, DefaultA[DefaultA[Unit]]]]
    .useRef(none[DefaultA[Unit]])
    .useEffectWithDepsBy((props, _) => props.deps)((props, cleanupEffect) =>
      deps =>
        props
          .fromDeps(deps)
          .flatMap(f => cleanupEffect.setAsync(f.some))
          .runAsyncAndForget
          .as( // React guarantees running the cleanup before the next effect, so we have the right value in the ref here.
            cleanupEffect.get.flatMap(_.foldMap(_.runAsyncAndForget))
          )
    )
    .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      final def useAsyncEffectWithDeps[D: Reusability, A](
        deps:   => D
      )(effect: D => DefaultA[DefaultA[Unit]])(implicit
        step:   Step
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => deps)(_ => effect)

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
      final def useAsyncEffectOnMount[A](effect: DefaultA[DefaultA[Unit]])(implicit
        step:                                    Step
      ): step.Self =
        useAsyncEffectOnMountBy(_ => effect)

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      final def useAsyncEffectWithDepsBy[D: Reusability, A](
        deps:   Ctx => D
      )(effect: Ctx => D => DefaultA[DefaultA[Unit]])(implicit
        step:   Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook[D]
          hookInstance(WithDeps(deps(ctx), effect(ctx)))
        }

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      final def useAsyncEffectBy[A](effect: Ctx => DefaultA[DefaultA[Unit]])(implicit
        step:                               Step
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => NeverReuse)(ctx => _ => effect(ctx))

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      final def useAsyncEffectOnMountBy[A](effect: Ctx => DefaultA[DefaultA[Unit]])(implicit
        step:                                      Step
      ): step.Self = // () has Reusability = always.
        useAsyncEffectWithDepsBy(_ => ())(ctx => _ => effect(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      def useAsyncEffectWithDepsBy[D: Reusability, A](
        deps:   CtxFn[D]
      )(effect: CtxFn[D => DefaultA[DefaultA[Unit]]])(implicit
        step:   Step
      ): step.Self =
        useAsyncEffectWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      def useAsyncEffectBy[A](effect: CtxFn[DefaultA[DefaultA[Unit]]])(implicit
        step:                         Step
      ): step.Self =
        useAsyncEffectBy(step.squash(effect)(_))

      /** Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
        * without a cleanup callback, just use the regular `useEffect` hook.
        */
      def useAsyncEffectOnMountBy[A](effect: CtxFn[DefaultA[DefaultA[Unit]]])(implicit
        step:                                Step
      ): step.Self =
        useAsyncEffectOnMountBy(step.squash(effect)(_))
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
