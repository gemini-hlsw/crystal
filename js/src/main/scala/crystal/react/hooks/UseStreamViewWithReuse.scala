package crystal.react.hooks

import crystal.Pot
import crystal.react.ReuseView
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

import scala.reflect.ClassTag

object UseStreamViewWithReuse {
  def hook[D: Reusability, A: ClassTag: Reusability] =
    CustomHook[(D, D => fs2.Stream[DefaultA, A])]
      .useStreamViewBy(props => props._1)(props => deps => props._2(deps))
      .buildReturning((_, view) => view.map(_.reuseByValue))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[ReuseView[A]]` so that the value can also be changed locally. Will
        * rerender when the `Pot` state changes. The fiber will be cancelled on unmount or deps
        * change.
        */
      final def useStreamViewWithReuse[D: Reusability, A: ClassTag: Reusability](
        deps:   => D
      )(stream: D => fs2.Stream[DefaultA, A])(implicit
        step:   Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamViewWithReuseBy(_ => deps)(_ => stream)

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount Provides pulled values as a
        * `Pot[ReuseView[A]]` so that the value can also be changed locally. Will rerender when the
        * `Pot` state changes. The fiber will be cancelled on unmount.
        */
      final def useStreamViewWithReuseOnMount[A: ClassTag: Reusability](
        stream: fs2.Stream[DefaultA, A]
      )(implicit
        step:   Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamViewWithReuseOnMountBy(_ => stream)

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[ReuseView[A]]` so that the value can also be changed locally. Will
        * rerender when the `Pot` state changes. The fiber will be cancelled on unmount or deps
        * change.
        */
      final def useStreamViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](deps: Ctx => D)(
        stream:                                                                          Ctx => D => fs2.Stream[DefaultA, A]
      )(implicit step:                                                                   Step): step.Next[Pot[ReuseView[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance((deps(ctx), stream(ctx)))
        }

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount Provides pulled values as a
        * `Pot[ReuseView[A]]` so that the value can also be changed locally. Will rerender when the
        * `Pot` state changes. The fiber will be cancelled on unmount.
        */
      final def useStreamViewWithReuseOnMountBy[A: ClassTag: Reusability](
        stream: Ctx => fs2.Stream[DefaultA, A]
      )(implicit
        step:   Step
      ): step.Next[Pot[ReuseView[A]]] = // () has Reusability = always.
        useStreamViewWithReuseBy(_ => ())(ctx => _ => stream(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[ReuseView[A]]` so that the value can also be changed locally. Will
        * rerender when the `Pot` state changes. The fiber will be cancelled on unmount or deps
        * change.
        */
      def useStreamViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](deps: CtxFn[D])(
        stream:                                                                    CtxFn[D => fs2.Stream[DefaultA, A]]
      )(implicit step:                                                             Step): step.Next[Pot[ReuseView[A]]] =
        useStreamViewWithReuseBy(step.squash(deps)(_))(step.squash(stream)(_))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount Provides pulled values as a
        * `Pot[ReuseView[A]]` so that the value can also be changed locally. Will rerender when the
        * `Pot` state changes. The fiber will be cancelled on unmount.
        */
      final def useStreamViewWithReuseOnMountBy[A: ClassTag: Reusability](
        stream: CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit
        step:   Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamViewWithReuseOnMountBy(step.squash(stream)(_))
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
