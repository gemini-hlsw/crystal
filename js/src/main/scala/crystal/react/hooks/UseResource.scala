package crystal.react.hooks

import cats.effect.Resource
import crystal.Pot
import crystal.implicits._
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseResource {
  def hook[D: Reusability, A] = CustomHook[(D, D => Resource[DefaultA, A])]
    .useState(Pot.pending[A])
    .useAsyncEffectWithDepsBy((props, _) => props._1)((props, state) =>
      deps =>
        (for {
          _             <- state.setStateAsync(Pot.pending)
          resource      <- props._2(deps).allocated
          (value, close) = resource
          _             <- state.setStateAsync(value.ready)
        } yield close)
          .handleErrorWith(t => state.setStateAsync(Pot.error(t)).as(DefaultA.delay(())))
    )
    .buildReturning((_, state) => state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Open a `Resource[Async, A]` on mount or when dependencies change, and close it on unmount
        * or when dependencies change. Provided as a `Pot[A]`. Will rerender when the `Pot` state
        * changes.
        */
      final def useResource[D: Reusability, A](
        deps:     => D
      )(resource: D => Resource[DefaultA, A])(implicit
        step:     Step
      ): step.Next[Pot[A]] =
        useResourceBy(_ => deps)(_ => resource)

      /** Open a `Resource[Async, A]` on mount and close it on unmount. Provided as a `Pot[A]`. Will
        * rerender when the `Pot` state changes.
        */
      final def useResourceOnMount[A](resource: Resource[DefaultA, A])(implicit
        step:                                   Step
      ): step.Next[Pot[A]] =
        useResourceOnMountBy(_ => resource)

      /** Open a `Resource[Async, A]` on mount or when dependencies change, and close it on unmount
        * or when dependencies change. Provided as a `Pot[A]`. Will rerender when the `Pot` state
        * changes.
        */
      final def useResourceBy[D: Reusability, A](
        deps:     Ctx => D
      )(resource: Ctx => D => Resource[DefaultA, A])(implicit
        step:     Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance((deps(ctx), resource(ctx)))
        }

      /** Open a `Resource[Async, A]` on mount and close it on unmount. Provided as a `Pot[A]`. Will
        * rerender when the `Pot` state changes.
        */
      final def useResourceOnMountBy[A](resource: Ctx => Resource[DefaultA, A])(implicit
        step:                                     Step
      ): step.Next[Pot[A]] = // () has Reusability = always.
        useResourceBy(_ => ())(ctx => _ => resource(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Open a `Resource[Async, A]` on mount or when dependencies change, and close it on unmount
        * or when dependencies change. Provided as a `Pot[A]`. Will rerender when the `Pot` state
        * changes.
        */
      def useResourceBy[D: Reusability, A](
        deps:     CtxFn[D]
      )(resource: CtxFn[D => Resource[DefaultA, A]])(implicit
        step:     Step
      ): step.Next[Pot[A]] =
        useResourceBy(step.squash(deps)(_))(step.squash(resource)(_))

      /** Open a `Resource[Async, A]` on mount and close it on unmount. Provided as a `Pot[A]`. Will
        * rerender when the `Pot` state changes.
        */
      final def useResourceOnMountBy[A](resource: CtxFn[Resource[DefaultA, A]])(implicit
        step:                                     Step
      ): step.Next[Pot[A]] =
        useResourceOnMountBy(step.squash(resource)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtResource1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtResource2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
