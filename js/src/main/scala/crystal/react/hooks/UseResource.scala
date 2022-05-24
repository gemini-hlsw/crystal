package crystal.react.hooks

import cats.effect.Resource
import crystal.Pot
import crystal.implicits._
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

object UseResource {
  def hook[A] = CustomHook[Resource[DefaultA, A]]
    .useState(Pot.pending[A])
    .useAsyncEffectOnMountBy((resource, state) =>
      resource.allocated
        .flatMap { case (value, close) =>
          state.setStateAsync(value.ready).as(close)
        }
        .handleErrorWith(t => state.setStateAsync(Pot.error(t)).as(DefaultA.delay(())))
    )
    .buildReturning((_, state) => state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Open a `Resource[Async, A] on mount and close it on unmount. Provided as a `Pot[A]`. Will
        * rerender when the `Pot` state changes.
        */
      final def useResource[A](resource: Resource[DefaultA, A])(implicit
        step:                            Step
      ): step.Next[Pot[A]] =
        useResourceBy(_ => resource)

      /** Creates component state that is reused while it's not updated. */
      final def useResourceBy[A](resource: Ctx => Resource[DefaultA, A])(implicit
        step:                              Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(resource(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state that is reused while it's not updated. */
      def useResourceBy[A](resource: CtxFn[Resource[DefaultA, A]])(implicit
        step:                        Step
      ): step.Next[Pot[A]] =
        useResourceBy(step.squash(resource)(_))
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
