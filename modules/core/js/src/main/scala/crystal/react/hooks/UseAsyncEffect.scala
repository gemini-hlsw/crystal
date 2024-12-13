// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.react.*
import crystal.react.reuse.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

object UseAsyncEffect {

  /**
   * Run async effect and cancel previously running instances, thus avoiding race conditions. Allows
   * returning a cleanup effect.
   */
  final def useAsyncEffectWithDeps[G, D: Reusability](deps: => D)(effect: D => G)(using
    G: EffectWithCleanup[G, DefaultA]
  ): HookResult[Unit] =
    // hookBuilder(WithDeps(deps, effect))
    useSingleEffect.flatMap: dispatcher =>
      useEffectWithDeps(deps): deps =>
        dispatcher.submit(effect(deps).normalize)

  /**
   * Run async effect and cancel previously running instances, thus avoiding race conditions. Allows
   * returning a cleanup effect.
   */
  final inline def useAsyncEffect[G](effect: => G)(using
    G: EffectWithCleanup[G, DefaultA]
  ): HookResult[Unit] =
    useAsyncEffectWithDeps(NeverReuse)((_: Reuse[Unit]) => effect)

  /**
   * Run async effect and cancel previously running instances, thus avoiding race conditions. Allows
   * returning a cleanup effect.
   */
  final inline def useAsyncEffectOnMount[G](effect: => G)(using
    G: EffectWithCleanup[G, DefaultA]
  ): HookResult[Unit] = // () has Reusability = always.
    useAsyncEffectWithDeps(())((_: Unit) => effect)

  // *** The rest is to support builder-style hooks *** //

  private def hook[G, D: Reusability](using
    EffectWithCleanup[G, DefaultA]
  ): CustomHook[WithDeps[D, G], Unit] =
    CustomHook.fromHookResult(input => useAsyncEffectWithDeps(input.deps)(input.fromDeps))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
       */
      final def useAsyncEffectWithDeps[G, D: Reusability](deps: => D)(effect: D => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => deps)(_ => effect)

      /**
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
       */
      final def useAsyncEffect[G](effect: G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectBy(_ => effect)

      /**
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
       */
      final def useAsyncEffectOnMount[G](effect: G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectOnMountBy(_ => effect)

      /**
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
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
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
       */
      final def useAsyncEffectBy[G](effect: Ctx => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => NeverReuse)(ctx => (_: Reuse[Unit]) => effect(ctx))

      /**
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
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
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
       */
      def useAsyncEffectWithDepsBy[G, D: Reusability](deps: CtxFn[D])(effect: CtxFn[D => G])(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
       */
      def useAsyncEffectBy[G](
        effect: CtxFn[G]
      )(using step: Step, G: EffectWithCleanup[G, DefaultA]): step.Self =
        useAsyncEffectBy(step.squash(effect)(_))

      /**
       * Run async effect and cancel previously running instances, thus avoiding race conditions.
       * Allows returning a cleanup effect.
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
