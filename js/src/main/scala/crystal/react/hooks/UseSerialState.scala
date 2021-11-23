package crystal.react.hooks

import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

case class UseSerialState[A] protected[hooks] (
  private val state: Hooks.UseState[SerialState[A]]
) {
  lazy val value: A = state.value.value

  def modState(f: A => A): DefaultS[Unit] = state.modState(_.update(f))

  def setState(a: A): DefaultS[Unit] = state.modState(_.update(_ => a))
}

object UseSerialState {
  def hook[A] = CustomHook[A]
    .useStateBy(initialValue => SerialState.initial(initialValue))
    .buildReturning((_, serialState) => UseSerialState(serialState))

  implicit def reuseUseSerialState[A]: Reusability[UseSerialState[A]] =
    Reusability.by(_.state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state that is reused while it's not updated. */
      final def useSerialState[A](initialValue: => A)(implicit
        step:                                   Step
      ): step.Next[UseSerialState[A]] =
        useSerialStateBy(_ => initialValue)

      /** Creates component state that is reused while it's not updated. */
      final def useSerialStateBy[A](initialValue: Ctx => A)(implicit
        step:                                     Step
      ): step.Next[UseSerialState[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(initialValue(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state that is reused while it's not updated. */
      def useSerialStateBy[A](initialValue: CtxFn[A])(implicit
        step:                               Step
      ): step.Next[UseSerialState[A]] =
        useSerialStateBy(step.squash(initialValue)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtSerialState1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtSerialState2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
