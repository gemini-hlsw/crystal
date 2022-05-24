package crystal.react.hooks

import crystal.Pot
import crystal.react.ReuseView
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

import scala.reflect.ClassTag

object UseStreamViewWithReuse {
  def hook[A: ClassTag: Reusability] =
    CustomHook[fs2.Stream[DefaultA, A]]
      .useStreamViewBy(props => props)
      .buildReturning((_, view) => view.map(_.reuseByValue))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      final def useStreamViewWithReuse[A: ClassTag: Reusability](stream: fs2.Stream[DefaultA, A])(
        implicit step:                                                   Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamViewWithReuseBy(_ => stream)

      final def useStreamViewWithReuseBy[A: ClassTag: Reusability](
        stream:        Ctx => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[Pot[ReuseView[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(stream(ctx))
        }

    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      def useStreamViewWithReuseBy[A: ClassTag: Reusability](
        stream:        CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit step: Step): step.Next[Pot[ReuseView[A]]] =
        useStreamViewWithReuseBy(step.squash(stream)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStreamViewWithReuse1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStreamViewWithReuse2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                             CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
