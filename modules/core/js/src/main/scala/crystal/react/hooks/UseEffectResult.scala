// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.*
import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

object UseEffectResult {
  private case class Input[A](effect: DefaultA[A], keep: Boolean)

  private def hook[D: Reusability, A] =
    CustomHook[WithDeps[D, Input[A]]]
      .useState(Pot.pending[A])
      .useMemoBy((props, _) => props.deps): (props, _) =>
        deps => props.fromDeps(deps)
      .useEffectWithDepsBy((_, _, input) => input): (_, state, _) =>
        input => state.setState(Pot.pending).unless(input.keep).void
      .useAsyncEffectWithDepsBy((_, _, input) => input): (_, state, _) =>
        input =>
          (for {
            a <- input.effect
            _ <- state.setStateAsync(a.ready)
          } yield ()).handleErrorWith(t => state.setStateAsync(Pot.error(t)))
      .buildReturning((_, state, _) => state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, reverts to `Pending` while executing the new effect.
       */
      final def useEffectResultWithDeps[D: Reusability, A](
        deps: => D
      )(effect: D => DefaultA[A])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectResultWithDepsBy(_ => deps)(_ => effect)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, keeps the old value while executing the new effect.
       */
      final def useEffectKeepResultWithDeps[D: Reusability, A](
        deps: => D
      )(effect: D => DefaultA[A])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectKeepResultWithDepsBy(_ => deps)(_ => effect)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMount[A](effect: DefaultA[A])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectResultOnMountBy(_ => effect)

      private def useEffectResultInternalWithDepsBy[D: Reusability, A](
        deps: Ctx => D
      )(effect: Ctx => D => DefaultA[A], keep: Boolean)(using
        step: Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance(WithDeps(deps(ctx), effect(ctx).andThen(Input(_, keep))))
        }

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, reverts to `Pending` while executing the new effect.
       */
      final def useEffectResultWithDepsBy[D: Reusability, A](
        deps: Ctx => D
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectResultInternalWithDepsBy(deps)(effect, keep = false)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, keeps the old value while executing the new effect.
       */
      final def useEffectKeepResultWithDepsBy[D: Reusability, A](
        deps: Ctx => D
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectResultInternalWithDepsBy(deps)(effect, keep = true)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMountBy[A](effect: Ctx => DefaultA[A])(using
        step: Step
      ): step.Next[Pot[A]] = // () has Reusability = always.
        useEffectResultWithDepsBy(_ => ())(ctx => _ => effect(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, reverts to `Pending` while executing the new effect.
       */
      def useEffectResultWithDepsBy[D: Reusability, A](
        deps: CtxFn[D]
      )(effect: CtxFn[D => DefaultA[A]])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectResultWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, keeps the old value while executing the new effect.
       */
      def useEffectKeepResultWithDepsBy[D: Reusability, A](
        deps: CtxFn[D]
      )(effect: CtxFn[D => DefaultA[A]])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectKeepResultWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMountBy[A](effect: CtxFn[DefaultA[A]])(using
        step: Step
      ): step.Next[Pot[A]] =
        useEffectResultOnMountBy(step.squash(effect)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtEffectResult1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtEffectResult2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
