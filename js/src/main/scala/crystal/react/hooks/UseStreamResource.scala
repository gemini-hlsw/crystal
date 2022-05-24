package crystal.react.hooks

import cats.effect.Resource
import crystal.Pot
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseStreamResource {
  def hook[A] = CustomHook[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
    .useState(Pot.pending[A])
    .useResourceBy((streamResource, state) =>
      streamResource.flatMap(stream => streamEvaluationResource(stream, state.setStateAsync))
    )
    .buildReturning((_, state, _) => state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      final def useStreamResource[A](streamResource: Resource[DefaultA, fs2.Stream[DefaultA, A]])(
        implicit step:                               Step
      ): step.Next[Pot[A]] =
        useStreamResourceBy(_ => streamResource)

      final def useStreamResourceBy[A](
        streamResource: Ctx => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(streamResource(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      def useStreamResourceBy[A](
        streamResource: CtxFn[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit
        step:           Step
      ): step.Next[Pot[A]] =
        useStreamResourceBy(step.squash(streamResource)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStreamResource1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStreamResource2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                        CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
