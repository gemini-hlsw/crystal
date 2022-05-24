package crystal.react.hooks

import crystal.Pot
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseStream {
  def hook[A] = CustomHook[fs2.Stream[DefaultA, A]]
    .useState(Pot.pending[A])
    .useResourceBy((stream, state) => streamEvaluationResource(stream, state.setStateAsync))
    .buildReturning((_, state, _) => state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      final def useStream[A](stream: fs2.Stream[DefaultA, A])(implicit
        step:                        Step
      ): step.Next[Pot[A]] =
        useStreamBy(_ => stream)

      final def useStreamBy[A](stream: Ctx => fs2.Stream[DefaultA, A])(implicit
        step:                          Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(stream(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      def useStreamBy[A](stream: CtxFn[fs2.Stream[DefaultA, A]])(implicit
        step:                    Step
      ): step.Next[Pot[A]] =
        useStreamBy(step.squash(stream)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStream1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStream2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
