// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.{Sync => DefaultS}

case class UseSerialState[A] protected[hooks] (
  private val state: Hooks.UseState[SerialState[A]]
) {
  lazy val value: Reusable[A] = Reusable.implicitly(state.value).map(_.value)

  val modState: Reusable[(A => A) => DefaultS[Unit]] =
    state.modState.map(mod => f => mod(_.update(f)))

  val setState: Reusable[A => DefaultS[Unit]] =
    state.modState.map(mod => a => mod(_.update(_ => a)))
}

object UseSerialState {
  def hook[A] = CustomHook[A]
    .useStateBy(initialValue => SerialState.initial(initialValue))
    .buildReturning((_, serialState) => UseSerialState(serialState))

  given [A]: Reusability[UseSerialState[A]] =
    Reusability.by(_.state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state that is reused while it's not updated. */
      final def useSerialState[A](initialValue: => A)(using
        step: Step
      ): step.Next[UseSerialState[A]] =
        useSerialStateBy(_ => initialValue)

      /** Creates component state that is reused while it's not updated. */
      final def useSerialStateBy[A](initialValue: Ctx => A)(using
        step: Step
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
      def useSerialStateBy[A](initialValue: CtxFn[A])(using
        step: Step
      ): step.Next[UseSerialState[A]] =
        useSerialStateBy(step.squash(initialValue)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtSerialState1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtSerialState2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
