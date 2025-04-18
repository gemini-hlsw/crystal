// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Resource
import crystal.*
import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

object UseResource:
  /**
   * Open a `Resource[Async, A]` on mount or when dependencies change, and close it on unmount or
   * when dependencies change. Provided as a `Pot[A]`. Will rerender when the `Pot` state changes.
   */
  final def useResource[D: Reusability, A](deps: => D)(
    resource: D => Resource[DefaultA, A]
  ): HookResult[Pot[A]] =
    for
      state <- useState(Pot.pending[A])
      _     <- useAsyncEffectWithDeps(deps): deps =>
                 (for
                   (value, close) <- resource(deps).allocated
                   _              <- state.setStateAsync(value.ready)
                 yield close).handleErrorWith: t =>
                   state.setStateAsync(Pot.error(t)).as(DefaultA.delay(()))
    yield state.value

  /**
   * Open a `Resource[Async, A]` on mount and close it on unmount. Provided as a `Pot[A]`. Will
   * rerender when the `Pot` state changes.
   */
  final inline def useResourceOnMount[A](resource: Resource[DefaultA, A]): HookResult[Pot[A]] =
    useResource(())(_ => resource)

  // *** The rest is to support builder-style hooks *** //

  private def hook[D: Reusability, A]: CustomHook[WithDeps[D, Resource[DefaultA, A]], Pot[A]] =
    CustomHook.fromHookResult(input => useResource(input.deps)(input.fromDeps))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Open a `Resource[Async, A]` on mount or when dependencies change, and close it on unmount
       * or when dependencies change. Provided as a `Pot[A]`. Will rerender when the `Pot` state
       * changes.
       */
      final def useResource[D: Reusability, A](
        deps: => D
      )(resource: D => Resource[DefaultA, A])(using
        step: Step
      ): step.Next[Pot[A]] =
        useResourceBy(_ => deps)(_ => resource)

      /**
       * Open a `Resource[Async, A]` on mount and close it on unmount. Provided as a `Pot[A]`. Will
       * rerender when the `Pot` state changes.
       */
      final def useResourceOnMount[A](resource: Resource[DefaultA, A])(using
        step: Step
      ): step.Next[Pot[A]] =
        useResourceOnMountBy(_ => resource)

      /**
       * Open a `Resource[Async, A]` on mount or when dependencies change, and close it on unmount
       * or when dependencies change. Provided as a `Pot[A]`. Will rerender when the `Pot` state
       * changes.
       */
      final def useResourceBy[D: Reusability, A](
        deps: Ctx => D
      )(resource: Ctx => D => Resource[DefaultA, A])(using
        step: Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance(WithDeps(deps(ctx), resource(ctx)))
        }

      /**
       * Open a `Resource[Async, A]` on mount and close it on unmount. Provided as a `Pot[A]`. Will
       * rerender when the `Pot` state changes.
       */
      final def useResourceOnMountBy[A](resource: Ctx => Resource[DefaultA, A])(using
        step: Step
      ): step.Next[Pot[A]] = // () has Reusability = always.
        useResourceBy(_ => ())(ctx => _ => resource(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Open a `Resource[Async, A]` on mount or when dependencies change, and close it on unmount
       * or when dependencies change. Provided as a `Pot[A]`. Will rerender when the `Pot` state
       * changes.
       */
      def useResourceBy[D: Reusability, A](
        deps: CtxFn[D]
      )(resource: CtxFn[D => Resource[DefaultA, A]])(using
        step: Step
      ): step.Next[Pot[A]] =
        useResourceBy(step.squash(deps)(_))(step.squash(resource)(_))

      /**
       * Open a `Resource[Async, A]` on mount and close it on unmount. Provided as a `Pot[A]`. Will
       * rerender when the `Pot` state changes.
       */
      final def useResourceOnMountBy[A](resource: CtxFn[Resource[DefaultA, A]])(using
        step: Step
      ): step.Next[Pot[A]] =
        useResourceOnMountBy(step.squash(resource)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtResource1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtResource2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
