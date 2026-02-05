// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.react.View
import crystal.react.syntax.view.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook

object UseSerialStateView:
  /** Creates component state as a View that is reused while it's not updated. */
  final def useSerialStateView[A](initialValue: => A): HookResult[Reusable[View[A]]] =
    useStateView(SerialState.initial(initialValue)).map:
      _.reusableBy(_.serial).map(_.zoom(_.value)(mod => _.update(mod)))

  // *** The rest is to support builder-style hooks *** //

  private def hook[A]: CustomHook[A, Reusable[View[A]]] =
    CustomHook.fromHookResult(useSerialStateView(_))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a View that is reused while it's not updated. */
      final def useSerialStateView[A](initialValue: => A)(using
        step: Step
      ): step.Next[Reusable[View[A]]] =
        useSerialStateViewBy(_ => initialValue)

      /** Creates component state as a View that is reused while it's not updated. */
      final def useSerialStateViewBy[A](initialValue: Ctx => A)(using
        step: Step
      ): step.Next[Reusable[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(initialValue(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state as a View that is reused while it's not updated. */
      def useSerialStateViewBy[A](initialValue: CtxFn[A])(using
        step: Step
      ): step.Next[Reusable[View[A]]] =
        useSerialStateViewBy(step.squash(initialValue)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtSerialStateView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtSerialStateView2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                         CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
