// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.react.View
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS

object UseStateView:
  /** Creates component state as a View */
  final def useStateView[A](initialValue: => A): HookResult[View[A]] =
    for
      state             <- useState(initialValue)
      stateRef          <- useShadowRef(state.value)
      onNextStateChange <- useStateCallback(state)
      modCB             <-
        useCallbackWithDeps((state.modState, onNextStateChange)): (modState, onNextStateChange) =>
          (f: A => A, cb: (A, A) => DefaultS[Unit]) =>
            stateRef.get >>= (previous => onNextStateChange(cb(previous, _)) >> modState(f))
    yield View[A](state.value, modCB)

  // *** The rest is to support builder-style hooks *** //

  private def hook[A]: CustomHook[A, View[A]] =
    CustomHook.fromHookResult(useStateView(_))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a View */
      final def useStateView[A](initialValue: => A)(using
        step: Step
      ): step.Next[View[A]] =
        useStateViewBy(_ => initialValue)

      /** Creates component state as a View */
      final def useStateViewBy[A](initialValue: Ctx => A)(using
        step: Step
      ): step.Next[View[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(initialValue(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state as a View */
      def useStateViewBy[A](initialValue: CtxFn[A])(using
        step: Step
      ): step.Next[View[A]] =
        useStateViewBy(step.squash(initialValue)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtStateView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStateView2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
