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
  def hook[D: Reusability, A: ClassTag: Reusability] =
    CustomHook[(D, D => Resource[DefaultA, fs2.Stream[DefaultA, A]])]
      .useStreamResourceViewBy(props => props._1)(props => deps => props._2(deps))
      .buildReturning((_, view) => view.map(_.reuseByValue))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[ReuseView[A]]` so
        * that the value can also be changed locally. Will rerender when the `Pot` state changes.
        * The fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuse[D: Reusability, A: ClassTag: Reusability](
        deps:           => D
      )(
        streamResource: D => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[ReuseView[A]]` so that the value can also be
        * changed locally. Will rerender when the `Pot` state changes. The fiber will be cancelled
        * and the resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseOnMount[A: ClassTag: Reusability](
        streamResource: Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:           Ctx => D
      )(
        streamResource: Ctx => D => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance((deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[ReuseView[A]]` so that the value can also be
        * changed locally. Will rerender when the `Pot` state changes. The fiber will be cancelled
        * and the resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseOnMountBy[A: ClassTag: Reusability](
        streamResource: Ctx => Resource[DefaultA, fs2.Stream[DefaultA, A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[ReuseView[A]]] = // () has Reusability = always.
        useStreamResourceViewWithReuseBy(_ => ())(ctx => _ => streamResource(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      def useStreamResourceViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:           CtxFn[D]
      )(
        streamResource: CtxFn[D => Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[ReuseView[A]]` so that the value can also be
        * changed locally. Will rerender when the `Pot` state changes. The fiber will be cancelled
        * and the resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseOnMountBy[A: ClassTag: Reusability](
        streamResource: CtxFn[Resource[DefaultA, fs2.Stream[DefaultA, A]]]
      )(implicit
        step:           Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseOnMountBy(step.squash(streamResource)(_))
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
