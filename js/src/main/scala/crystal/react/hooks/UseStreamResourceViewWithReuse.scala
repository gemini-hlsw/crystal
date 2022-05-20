package crystal.react.hooks

import cats.effect.Resource
import crystal.Pot
import crystal.react.ReuseView
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

import scala.reflect.ClassTag

object UseStreamResourceViewWithReuse {
  def hook[A: ClassTag: Reusability] =
    CustomHook[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      .useStreamResourceViewBy(streamResource => streamResource)
      .buildReturning((_, view) => view.map(_.reuseByValue))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      final def useStreamResourceViewWithReuse[A: ClassTag: Reusability](
        streamResource: Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(_ => streamResource)

      final def useStreamResourceViewWithReuseBy[A: ClassTag: Reusability](
        streamResource: Ctx => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(streamResource(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      def useStreamResourceViewWithReuseBy[A: ClassTag: Reusability](
        streamResource: CtxFn[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(step.squash(streamResource)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStreamResourceViewWithReuse1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStreamResourceViewWithReuse2[Ctx, CtxFn[
      _
    ], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
