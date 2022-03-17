package crystal.react.hooks

import crystal.react.View
import crystal.react.reuse._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook

object UseSerialStateView {
  def hook[A]: CustomHook[A, Reuse[View[A]]] = CustomHook[A]
    .useStateViewBy(initialValue => SerialState.initial(initialValue))
    .buildReturning((_, serialStateView) =>
      Reuse.by(serialStateView.get.serial)(serialStateView.zoom(_.value)(mod => _.update(mod)))
    )

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a View that is reused while it's not updated. */
      final def useSerialStateView[A](initialValue: => A)(implicit
        step:                                       Step
      ): step.Next[Reuse[View[A]]] =
        useSerialStateViewBy(_ => initialValue)

      /** Creates component state as a View that is reused while it's not updated. */
      final def useSerialStateViewBy[A](initialValue: Ctx => A)(implicit
        step:                                         Step
      ): step.Next[Reuse[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(initialValue(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state as a View that is reused while it's not updated. */
      def useSerialStateViewBy[A](initialValue: CtxFn[A])(implicit
        step:                                   Step
      ): step.Next[Reuse[View[A]]] =
        useSerialStateViewBy(step.squash(initialValue)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

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

  object implicits extends HooksApiExt
}
