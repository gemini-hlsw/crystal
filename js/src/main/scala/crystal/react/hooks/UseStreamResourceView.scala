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
  def hook[D: Reusability, A] =
    CustomHook[(D, D => Resource[DefaultA, fs2.Stream[DefaultA, A]])]
      .useState(Pot.pending[A])
      .useStateCallbackBy((_, state) => state)
      .useResourceBy((props, _, _) => props._1)((props, state, _) =>
        deps =>
          props._2(deps).flatMap(stream => streamEvaluationResource(stream, state.setStateAsync))
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

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceView[D: Reusability, A](deps: => D)(
        streamResource:                                        D => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:                                         Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewOnMount[A](
        streamResource: Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[View[A]]] =
        useStreamResourceViewOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewBy[D: Reusability, A](deps: Ctx => D)(
        streamResource:                                          Ctx => D => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:                                           Step): step.Next[Pot[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance((deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewOnMountBy[A](
        streamResource: Ctx => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[View[A]]] = // () has Reusability = always.
        useStreamResourceViewBy(_ => ())(ctx => _ => streamResource(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      def useStreamResourceViewBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource:                                    CtxFn[D => Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit step:                                     Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewOnMountBy[A](
        streamResource: CtxFn[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit
        step:           Step
      ): step.Next[Pot[View[A]]] =
        useStreamResourceViewOnMountBy(step.squash(streamResource)(_))
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
