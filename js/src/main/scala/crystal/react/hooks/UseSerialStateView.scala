// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.react.ReuseView
import crystal.react.reuse.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook

object UseSerialStateView {
  def hook[A]: CustomHook[A, ReuseView[A]] = CustomHook[A]
    .useStateViewBy(initialValue => SerialState.initial(initialValue))
    .buildReturning((_, serialStateView) =>
      Reuse.by(serialStateView.get.serial)(serialStateView.zoom(_.value)(mod => _.update(mod)))
    )

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a View that is reused while it's not updated. */
      final def useSerialStateView[A](initialValue: => A)(using
        step: Step
      ): step.Next[ReuseView[A]] =
        useSerialStateViewBy(_ => initialValue)

      /** Creates component state as a View that is reused while it's not updated. */
      final def useSerialStateViewBy[A](initialValue: Ctx => A)(using
        step: Step
      ): step.Next[ReuseView[A]] =
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
      ): step.Next[ReuseView[A]] =
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
}
