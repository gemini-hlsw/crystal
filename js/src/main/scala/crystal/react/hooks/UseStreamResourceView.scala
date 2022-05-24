package crystal.react.hooks

import cats.effect.Resource
import cats.syntax.all._
import crystal.Pot
import crystal.react.View
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

object UseStreamResourceView {
  def hook[A] =
    CustomHook[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      .useState(Pot.pending[A])
      .useStateCallbackBy((_, state) => state)
      .useResourceBy((streamResource, state, _) =>
        streamResource.flatMap(stream => streamEvaluationResource(stream, state.setStateAsync))
      )
      .buildReturning { (_, state, delayedCallback, _) =>
        state.value.map { a =>
          View[A](
            a,
            (f: A => A, cb: A => DefaultS[Unit]) =>
              state.modState(_.map(f)) >> delayedCallback(_.toOption.foldMap(cb))
          )
        }
      }

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      final def useStreamResourceView[A](
        streamResource: Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:  Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(_ => streamResource)

      final def useStreamResourceViewBy[A](
        streamResource: Ctx => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:  Step): step.Next[Pot[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(streamResource(ctx))
        }

    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {
      def useStreamResourceViewBy[A](
        streamResource: CtxFn[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit step:  Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(step.squash(streamResource)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStreamResourceView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStreamResourceView2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                            CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
