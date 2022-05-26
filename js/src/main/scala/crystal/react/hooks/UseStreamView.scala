package crystal.react.hooks

import cats.effect.kernel.Resource
import crystal.Pot
import crystal.react.View
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseStreamView {
  def hook[D: Reusability, A] =
    CustomHook[(D, D => fs2.Stream[DefaultA, A])]
      .useStreamResourceViewBy(props => props._1)(props => deps => Resource.pure(props._2(deps)))
      .buildReturning((_, view) => view)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[View[A]]` so that the value can also be changed locally. Will
        * rerender when the `Pot` state changes. The fiber will be cancelled on unmount or deps
        * change.
        */
      final def useStreamView[D: Reusability, A](deps: => D)(stream: D => fs2.Stream[DefaultA, A])(
        implicit step:                                 Step
      ): step.Next[Pot[View[A]]] =
        useStreamViewBy(_ => deps)(_ => stream)

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount Provides pulled values as a
        * `Pot[View[A]]` so that the value can also be changed locally. Will rerender when the `Pot`
        * state changes. The fiber will be cancelled on unmount.
        */
      final def useStreamViewOnMount[A](stream: fs2.Stream[DefaultA, A])(implicit
        step:                                   Step
      ): step.Next[Pot[View[A]]] =
        useStreamViewOnMountBy(_ => stream)

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[View[A]]` so that the value can also be changed locally. Will
        * rerender when the `Pot` state changes. The fiber will be cancelled on unmount or deps
        * change.
        */
      final def useStreamViewBy[D: Reusability, A](
        deps:   Ctx => D
      )(stream: Ctx => D => fs2.Stream[DefaultA, A])(implicit
        step:   Step
      ): step.Next[Pot[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance((deps(ctx), stream(ctx)))
        }

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount Provides pulled values as a
        * `Pot[View[A]]` so that the value can also be changed locally. Will rerender when the `Pot`
        * state changes. The fiber will be cancelled on unmount.
        */
      final def useStreamViewOnMountBy[A](stream: Ctx => fs2.Stream[DefaultA, A])(implicit
        step:                                     Step
      ): step.Next[Pot[View[A]]] = // () has Reusability = always.
        useStreamViewBy(_ => ())(ctx => _ => stream(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[View[A]]` so that the value can also be changed locally. Will
        * rerender when the `Pot` state changes. The fiber will be cancelled on unmount or deps
        * change.
        */
      def useStreamViewBy[D: Reusability, A](
        deps:   CtxFn[D]
      )(stream: CtxFn[D => fs2.Stream[DefaultA, A]])(implicit
        step:   Step
      ): step.Next[Pot[View[A]]] =
        useStreamViewBy(step.squash(deps)(_))(step.squash(stream)(_))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount Provides pulled values as a
        * `Pot[View[A]]` so that the value can also be changed locally. Will rerender when the `Pot`
        * state changes. The fiber will be cancelled on unmount.
        */
      final def useStreamViewOnMountBy[A](stream: CtxFn[fs2.Stream[DefaultA, A]])(implicit
        step:                                     Step
      ): step.Next[Pot[View[A]]] =
        useStreamViewOnMountBy(step.squash(stream)(_))
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
