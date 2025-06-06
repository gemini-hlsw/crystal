// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.syntax.all.*
import crystal.*
import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS

class UseEffectResult[A](
  val value:       Pot[A],
  val isRunning:   Boolean,
  refreshInternal: Reusable[Boolean => DefaultS[Unit]],
  defaultKeep:     Boolean
):
  val refresh: Reusable[DefaultS[Unit]]       = refreshInternal.map(_(defaultKeep))
  val refreshKeep: Reusable[DefaultS[Unit]]   = refreshInternal.map(_(true))
  val refreshNoKeep: Reusable[DefaultS[Unit]] = refreshInternal.map(_(false))

object UseEffectResult:
  private case class Input[D, A, R](
    effect: WithPotDeps[D, DefaultA[A], R],
    keep:   Boolean
  ):
    val depsOpt: Option[D] = effect.deps.toOption

  private def doRefresh[A](
    effect:     DefaultA[A],
    set:        Pot[A] => DefaultS[Unit],
    setRunning: Boolean => DefaultS[Unit]
  )(
    keep:       Boolean
  ): DefaultS[Unit] =
    set(Pot.pending).unless(keep).void >>
      setRunning(true) >>
      (effect >>= (a => set(a.ready).toAsync))
        .handleErrorWith(t => set(Pot.error(t)).toAsync)
        .guarantee(setRunning(false).toAsync)
        .runAsyncAndForget

  // Provides functionality for all the flavors
  private def hookBuilder[D, A, R: Reusability](
    deps: Pot[D]
  )(effect: D => DefaultA[A], keep: Boolean, reuseBy: Option[R]): HookResult[UseEffectResult[A]] =
    for
      state     <- useState(Pot.pending[A])
      isRunning <- useState(false)
      effectOpt <- useMemo(reuseBy): _ => // Memo Option[effect]
                     deps.toOption.map(effect)
      refresh    = effectOpt.map(_.foldMap(doRefresh(_, state.setState, isRunning.setState)))
      _         <- useEffectWithDeps(refresh)(_(keep))
    yield UseEffectResult(state.value, isRunning.value, refresh, keep)

  /**
   * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`. When
   * dependencies change, reverts to `Pending` while executing the new effect.
   */
  final inline def useEffectResultWithDeps[D: Reusability, A](
    deps: => D
  )(effect: D => DefaultA[A]): HookResult[UseEffectResult[A]] =
    hookBuilder(deps.ready)(effect, keep = false, deps.some)

  /**
   * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state (but not when
   * they change once `Ready`) and stores the result in a state, which is provided as a `Pot[A]`.
   * When dependencies change, reverts to `Pending` while executing the new effect or while waiting
   * for them to become `Ready` again. For multiple dependencies, use `(par1, par2, ...).tupled`.
   */
  final inline def useEffectResultWhenDepsReady[D, A](
    deps: => Pot[D]
  )(effect: D => DefaultA[A]): HookResult[UseEffectResult[A]] =
    hookBuilder(deps)(effect, keep = false, deps.toOption.void)

  /**
   * Runs an async effect when `Pot` dependencies transition into a `Ready` state or change once
   * `Ready` and stores the result in a state, which is provided as a `Pot[A]`. When dependencies
   * change, reverts to `Pending` while executing the new effect or while waiting for them to become
   * `Ready` again. For multiple dependencies, use `(par1, par2, ...).tupled`.
   */
  final inline def useEffectResultWhenDepsReadyOrChange[D: Reusability, A](
    deps: => Pot[D]
  )(effect: D => DefaultA[A]): HookResult[UseEffectResult[A]] =
    hookBuilder(deps)(effect, keep = false, deps.toOption)

  /**
   * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`. When
   * dependencies change, keeps the old value while executing the new effect.
   */
  final inline def useEffectKeepResultWithDeps[D: Reusability, A](
    deps: => D
  )(effect: D => DefaultA[A]): HookResult[UseEffectResult[A]] =
    hookBuilder(deps.ready)(effect, keep = true, deps.some)

  /**
   * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state (but not when
   * they change once `Ready`) and stores the result in a state, which is provided as a `Pot[A]`.
   * When dependencies change, keeps the old value while executing the new effect or while waiting
   * for them to become `Ready` again. For multiple dependencies, use `(par1, par2, ...).tupled`.
   */
  final inline def useEffectKeepResultWhenDepsReady[D, A](
    deps: => Pot[D]
  )(effect: D => DefaultA[A]): HookResult[UseEffectResult[A]] =
    hookBuilder(deps)(effect, keep = true, deps.toOption.void)

  /**
   * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state or change once
   * `Ready` and stores the result in a state, which is provided as a `Pot[A]`. When dependencies
   * change, keeps the old value while executing the new effect or while waiting for them to become
   * `Ready` again. For multiple dependencies, use `(par1, par2, ...).tupled`.
   */
  final inline def useEffectKeepResultWhenDepsReadyOrChange[D: Reusability, A](
    deps: => Pot[D]
  )(effect: D => DefaultA[A]): HookResult[UseEffectResult[A]] =
    hookBuilder(deps)(effect, keep = true, deps.toOption)

  /**
   * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
   */
  final inline def useEffectResultOnMount[A](
    effect: => DefaultA[A]
  ): HookResult[UseEffectResult[A]] =
    useEffectResultWithDeps(())(_ => effect) // () has Reusability = always.

  /**
   * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`. This
   * makes sense when doing manual refreshes.
   */
  final inline def useEffectKeepResultOnMount[A](
    effect: => DefaultA[A]
  ): HookResult[UseEffectResult[A]] =
    useEffectKeepResultWithDeps(())(_ => effect) // () has Reusability = always.

  // *** The rest is to support builder-style hooks *** //

  private def hook[D, A, R: Reusability]: CustomHook[Input[D, A, R], UseEffectResult[A]] =
    CustomHook.fromHookResult: input =>
      hookBuilder(input.effect.deps)(input.effect.fromDeps, input.keep, input.effect.reuseValue)
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
      ): step.Next[UseEffectResult[A]] =
        useEffectResultWithDepsBy(_ => deps)(_ => effect)

      /**
       * Runs an async effect when `Pot` dependencies transition into a `Ready` state and stores the
       * result in a state, which is provided as a `Pot[A]`. When dependencies change, reverts to
       * `Pending` while executing the new effect or while waiting for them to become `Ready` again.
       * For multiple dependencies, use `(par1, par2, ...).tupled`.
       */
      final def useEffectResultWhenDepsReady[D, A](
        deps: => Pot[D]
      )(effect: D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultWhenDepsReadyBy(_ => deps)(_ => effect)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, keeps the old value while executing the new effect.
       */
      final def useEffectKeepResultWithDeps[D: Reusability, A](
        deps: => D
      )(effect: D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectKeepResultWithDepsBy(_ => deps)(_ => effect)

      /**
       * Runs an async effect when `Pot` dependencies transition into a `Ready` state and stores the
       * result in a state, which is provided as a `Pot[A]`. When dependencies change, keeps the old
       * value while executing the new effect or while waiting for them to become `Ready` again. For
       * multiple dependencies, use `(par1, par2, ...).tupled`.
       */
      final def useEffectKeepResultWhenDepsReady[D, A](
        deps: => Pot[D]
      )(effect: D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectKeepResultWhenDepsReadyBy(_ => deps)(_ => effect)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMount[A](effect: DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultOnMountBy(_ => effect)

      private def useEffectResultInternalWithDepsBy[D: Reusability, A](
        deps: Ctx => D
      )(effect: Ctx => D => DefaultA[A], keep: Boolean)(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultInternalWhenDepsReadyOrChangeBy(deps.andThen(_.ready))(effect, keep)

      private def useEffectResultInternalWhenDepsReadyBy[D, A](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => DefaultA[A], keep: Boolean)(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A, Unit]
          hookInstance(Input(WithPotDeps.WhenReady(deps(ctx), effect(ctx)), keep))
        }

      private def useEffectResultInternalWhenDepsReadyOrChangeBy[D: Reusability, A](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => DefaultA[A], keep: Boolean)(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A, D]
          hookInstance(Input(WithPotDeps.WhenReadyOrChange(deps(ctx), effect(ctx)), keep))
        }

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, reverts to `Pending` while executing the new effect.
       */
      final def useEffectResultWithDepsBy[D: Reusability, A](
        deps: Ctx => D
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultInternalWithDepsBy(deps)(effect, keep = false)

      /**
       * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state (but not
       * when they change once `Ready`) and stores the result in a state, which is provided as a
       * `Pot[A]`. When dependencies change, reverts to `Pending` while executing the new effect or
       * while waiting for them to become `Ready` again. For multiple dependencies, use `(par1,
       * par2, ...).tupled`.
       */
      final def useEffectResultWhenDepsReadyBy[D, A](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultInternalWhenDepsReadyBy(deps)(effect, keep = false)

      /**
       * Runs an async effect when `Pot` dependencies transition into a `Ready` state or change once
       * `Ready` and stores the result in a state, which is provided as a `Pot[A]`. When
       * dependencies change, reverts to `Pending` while executing the new effect or while waiting
       * for them to become `Ready` again. For multiple dependencies, use `(par1, par2,
       * ...).tupled`.
       */
      final def useEffectResultWhenDepsReadyOrChangeBy[D: Reusability, A](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultInternalWhenDepsReadyOrChangeBy(deps)(effect, keep = false)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, keeps the old value while executing the new effect.
       */
      final def useEffectKeepResultWithDepsBy[D: Reusability, A](
        deps: Ctx => D
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultInternalWithDepsBy(deps)(effect, keep = true)

      /**
       * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state (but not
       * when they change once `Ready`) and stores the result in a state, which is provided as a
       * `Pot[A]`. When dependencies change, keeps the old value while executing the new effect or
       * while waiting for them to become `Ready` again. For multiple dependencies, use `(par1,
       * par2, ...).tupled`.
       */
      final def useEffectKeepResultWhenDepsReadyBy[D, A](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultInternalWhenDepsReadyBy(deps)(effect, keep = true)

      /**
       * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state or change
       * once `Ready` and stores the result in a state, which is provided as a `Pot[A]`. When
       * dependencies change, keeps the old value while executing the new effect or while waiting
       * for them to become `Ready` again. For multiple dependencies, use `(par1, par2,
       * ...).tupled`.
       */
      final def useEffectKeepResultWhenDepsReadyOrChangeBy[D: Reusability, A](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultInternalWhenDepsReadyOrChangeBy(deps)(effect, keep = true)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMountBy[A](effect: Ctx => DefaultA[A])(using
        step: Step
      ): step.Next[UseEffectResult[A]] = // () has Reusability = always.
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
      ): step.Next[UseEffectResult[A]] =
        useEffectResultWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state (but not
       * when they change once `Ready`) and stores the result in a state, which is provided as a
       * `Pot[A]`. When dependencies change, reverts to `Pending` while executing the new effect or
       * while waiting for them to become `Ready` again. For multiple dependencies, use `(par1,
       * par2, ...).tupled`.
       */
      def useEffectResultWhenDepsReadyBy[D, A](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => DefaultA[A]])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultWhenDepsReadyBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect when `Pot` dependencies transition into a `Ready` state or change once
       * `Ready` and stores the result in a state, which is provided as a `Pot[A]`. When
       * dependencies change, reverts to `Pending` while executing the new effect or while waiting
       * for them to become `Ready` again. For multiple dependencies, use `(par1, par2,
       * ...).tupled`.
       */
      def useEffectResultWhenDepsReadyOrChangeBy[D: Reusability, A](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => DefaultA[A]])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectResultWhenDepsReadyOrChangeBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       * When dependencies change, keeps the old value while executing the new effect.
       */
      def useEffectKeepResultWithDepsBy[D: Reusability, A](
        deps: CtxFn[D]
      )(effect: CtxFn[D => DefaultA[A]])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectKeepResultWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect whenever `Pot` dependencies transition into a `Ready` state (but not
       * when they change once `Ready`) and stores the result in a state, which is provided as a
       * `Pot[A]`. When dependencies change, keeps the old value while executing the new effect or
       * while waiting for them to become `Ready` again. For multiple dependencies, use `(par1,
       * par2, ...).tupled`.
       */
      def useEffectKeepResultWhenDepsReadyBy[D, A](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => DefaultA[A]])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectKeepResultWhenDepsReadyBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect when `Pot` dependencies transition into a `Ready` state or change once
       * `Ready` and stores the result in a state, which is provided as a `Pot[A]`. When
       * dependencies change, keeps the old value while executing the new effect or while waiting
       * for them to become `Ready` again. For multiple dependencies, use `(par1, par2,
       * ...).tupled`.
       */
      def useEffectKeepResultWhenDepsReadyOrChangeBy[D: Reusability, A](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => DefaultA[A]])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
        useEffectKeepResultWhenDepsReadyOrChangeBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMountBy[A](effect: CtxFn[DefaultA[A]])(using
        step: Step
      ): step.Next[UseEffectResult[A]] =
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
