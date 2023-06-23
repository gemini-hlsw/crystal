// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.kernel.Deferred
import cats.syntax.all.*
import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{Async => DefaultA}

object UseAsyncEffect {
  def hook[D: Reusability] = CustomHook[WithDeps[D, DefaultA[DefaultA[Unit]]]]
    .useRef(none[Deferred[DefaultA, DefaultA[Unit]]])
    .useEffectWithDepsBy((props, _) => props.deps)((props, cleanupEffect) =>
      deps =>
        (for {
          // The latch makes sure that the effect is executed before attempting cleanup.
          // Without the latch mechanism, the cleanup could be called before the effect ran
          // completely. Then the cleanup effect would still be empty and resources would leak.
          newLatch <- Deferred[DefaultA, DefaultA[Unit]]
          _        <- cleanupEffect.setAsync(newLatch.some)
          cleanup  <- props.fromDeps(deps)
          _        <- newLatch.complete(cleanup)
        } yield ()).runAsyncAndForget
          .as( // React guarantees running the cleanup before the next effect, so we have the right value in the ref here.
            cleanupEffect.get.flatMap(latchOpt =>
              latchOpt
                .map(latch =>
                  (for {
                    cleanup <- latch.get
                    _       <- cleanup
                  } yield ()).runAsyncAndForget
                )
                .orEmpty
            )
          )
    )
    .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectWithDeps[D: Reusability, A](
        deps: => D
      )(effect: D => DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => deps)(_ => effect)

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffect[A](effect: DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self =
        useAsyncEffectBy(_ => effect)

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectOnMount[A](effect: DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self =
        useAsyncEffectOnMountBy(_ => effect)

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectWithDepsBy[D: Reusability, A](
        deps: Ctx => D
      )(effect: Ctx => D => DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook[D]
          hookInstance(WithDeps(deps(ctx), effect(ctx)))
        }

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectBy[A](effect: Ctx => DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self =
        useAsyncEffectWithDepsBy(_ => NeverReuse)(ctx => _ => effect(ctx))

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      final def useAsyncEffectOnMountBy[A](effect: Ctx => DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self = // () has Reusability = always.
        useAsyncEffectWithDepsBy(_ => ())(ctx => _ => effect(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      def useAsyncEffectWithDepsBy[D: Reusability, A](
        deps: CtxFn[D]
      )(effect: CtxFn[D => DefaultA[DefaultA[Unit]]])(using
        step: Step
      ): step.Self =
        useAsyncEffectWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      def useAsyncEffectBy[A](effect: CtxFn[DefaultA[DefaultA[Unit]]])(using
        step: Step
      ): step.Self =
        useAsyncEffectBy(step.squash(effect)(_))

      /**
       * Simulates `useEffect` with cleanup callback for async effect. To declare an async effect
       * without a cleanup callback, just use the regular `useEffect` hook.
       */
      def useAsyncEffectOnMountBy[A](effect: CtxFn[DefaultA[DefaultA[Unit]]])(using
        step: Step
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
