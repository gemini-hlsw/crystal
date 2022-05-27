package crystal.react.hooks

import cats.effect.Resource
import crystal.Pot
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseStreamResource {
  def hook[D: Reusability, A] = CustomHook[(D, D => Resource[DefaultA, fs2.Stream[DefaultA, A]])]
    .useState(Pot.pending[A])
    .useEffectWithDepsBy((props, _) => props._1)((_, state) => _ => state.setState(Pot.pending))
    .useResourceBy((props, _) => props._1)((props, state) =>
      deps =>
        props._2(deps).flatMap(stream => streamEvaluationResource(stream, state.setStateAsync))
    )
    .buildReturning((_, state, resource) => resource.flatMap(_ => state.value))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[A]`. Will rerender
        * when the `Pot` state changes. The fiber will be cancelled and the resource closed on
        * unmount or deps change.
        */
      final def useStreamResource[D: Reusability, A](
        deps:           => D
      )(streamResource: D => Resource[DefaultA, fs2.Stream[DefaultA, A]])(implicit
        step:           Step
      ): step.Next[Pot[A]] =
        useStreamResourceBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes.
        * The fiber will be cancelled and the resource closed on unmount.
        */
      final def useStreamResourceOnMount[A](
        streamResource: Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[A]] =
        useStreamResourceOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[A]`. Will rerender
        * when the `Pot` state changes. The fiber will be cancelled and the resource closed on
        * unmount or deps change.
        */
      final def useStreamResourceBy[D: Reusability, A](deps: Ctx => D)(
        streamResource:                                      Ctx => D => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:                                                Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance((deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes.
        * The fiber will be cancelled and the resource closed on unmount.
        */
      final def useStreamResourceOnMountBy[A](
        streamResource: Ctx => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[A]] = // () has Reusability = always.
        useStreamResourceBy(_ => ())(ctx => _ => streamResource(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[A]`. Will rerender
        * when the `Pot` state changes. The fiber will be cancelled and the resource closed on
        * unmount or deps change.
        */
      def useStreamResourceBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource:                                CtxFn[D => Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit
        step:                                          Step
      ): step.Next[Pot[A]] =
        useStreamResourceBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes.
        * The fiber will be cancelled and the resource closed on unmount.
        */
      final def useStreamResourceOnMountBy[A](
        streamResource: CtxFn[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit
        step:           Step
      ): step.Next[Pot[A]] =
        useStreamResourceOnMountBy(step.squash(streamResource)(_))
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
