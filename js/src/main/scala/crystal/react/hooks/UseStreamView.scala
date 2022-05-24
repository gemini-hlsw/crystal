package crystal.react.hooks

import cats.effect.kernel.Resource
import crystal.Pot
import crystal.react.View
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseStreamView {
  def hook[A] =
    CustomHook[fs2.Stream[DefaultA, A]]
      .useStreamResourceViewBy(stream => Resource.pure(stream))
      .buildReturning((_, view) => view)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      final def useStreamView[A](stream: fs2.Stream[DefaultA, A])(implicit
        step:                            Step
      ): step.Next[Pot[View[A]]] =
        useStreamViewBy(_ => stream)

      final def useStreamViewBy[A](stream: Ctx => fs2.Stream[DefaultA, A])(implicit
        step:                              Step
      ): step.Next[Pot[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(stream(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      def useStreamViewBy[A](stream: CtxFn[fs2.Stream[DefaultA, A]])(implicit
        step:                        Step
      ): step.Next[Pot[View[A]]] =
        useStreamViewBy(step.squash(stream)(_))
    }

  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStreamView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStreamView2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
