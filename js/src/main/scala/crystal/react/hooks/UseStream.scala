package crystal.react.hooks

import crystal.Pot
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseStream {
  def hook[D: Reusability, A] = CustomHook[(D, D => fs2.Stream[DefaultA, A])]
    .useState(Pot.pending[A])
    .useResourceBy((props, _) => props._1)((props, state) =>
      deps => streamEvaluationResource(props._2(deps), state.setStateAsync)
    )
    .buildReturning((_, state, _) => state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStream[D: Reusability, A](
        deps:   => D
      )(stream: D => fs2.Stream[DefaultA, A])(implicit
        step:   Step
      ): step.Next[Pot[A]] =
        useStreamBy(_ => deps)(_ => stream)

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamOnMount[A](stream: fs2.Stream[DefaultA, A])(implicit
        step:                               Step
      ): step.Next[Pot[A]] =
        useStreamOnMountBy(_ => stream)

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamBy[D: Reusability, A](
        deps:   Ctx => D
      )(stream: Ctx => D => fs2.Stream[DefaultA, A])(implicit
        step:   Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance((deps(ctx), stream(ctx)))
        }

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamOnMountBy[A](stream: Ctx => fs2.Stream[DefaultA, A])(implicit
        step:                                 Step
      ): step.Next[Pot[A]] = // () has Reusability = always.
        useStreamBy(_ => ())(ctx => _ => stream(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      def useStreamBy[D: Reusability, A](
        deps:   CtxFn[D]
      )(stream: CtxFn[D => fs2.Stream[DefaultA, A]])(implicit
        step:   Step
      ): step.Next[Pot[A]] =
        useStreamBy(step.squash(deps)(_))(step.squash(stream)(_))

      /** Create a fiber on mount that drains a stream and provides pulled values as a `Pot[A]`.
        * Will rerender when the `Pot` state changes. The fiber will be cancelled on unmount.
        */
      final def useStreamOnMountBy[A](stream: CtxFn[fs2.Stream[DefaultA, A]])(implicit
        step:                                 Step
      ): step.Next[Pot[A]] =
        useStreamOnMountBy(step.squash(stream)(_))
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
