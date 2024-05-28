// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA
import crystal.react.reuse.*

object UseAsyncEffect {
  def hook[G, D: Reusability](using EffectWithCleanup[G, DefaultA]) =
    CustomHook[WithDeps[D, G]].useSingleEffect
      .useEffectWithDepsBy((props, _) => props.deps): (props, dispatcher) =>
        deps => dispatcher.submit(props.fromDeps(deps).normalize)
      .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {
      // TODO UPDATE SCALADOCS!!!
      // TODO UPDATE SCALADOCS!!!
      // TODO UPDATE SCALADOCS!!!
      // TODO UPDATE SCALADOCS!!!
      // TODO UPDATE SCALADOCS!!!

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectWithDeps[G, D: Reusability](deps: => D)(effect: D => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => deps)(_ => effect)

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffect[G](effect: G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectBy(_ => effect)

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectOnMount[G](effect: G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectOnMountBy(_ => effect)

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectWithDepsBy[G, D: Reusability](deps: Ctx => D)(effect: Ctx => D => G)(
        using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook[G, D]
          hookInstance(WithDeps(deps(ctx), effect(ctx)))
        }

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectBy[G](effect: Ctx => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => NeverReuse)(ctx => (_: Reuse[Unit]) => effect(ctx))

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectOnMountBy[G](effect: Ctx => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self = // () has Reusability = always.
        useAsyncEffectWithDepsBy(_ => ())(ctx => (_: Unit) => effect(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      def useAsyncEffectWithDepsBy[G, D: Reusability](deps: CtxFn[D])(effect: CtxFn[D => G])(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      def useAsyncEffectBy[G](
        effect: CtxFn[G]
      )(using step: Step, G: EffectWithCleanup[G, DefaultA]): step.Self =
        useAsyncEffectBy(step.squash(effect)(_))

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      def useAsyncEffectOnMountBy[G](effect: CtxFn[G])(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectOnMountBy(step.squash(effect)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtAsyncEffect1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtAsyncEffect2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
