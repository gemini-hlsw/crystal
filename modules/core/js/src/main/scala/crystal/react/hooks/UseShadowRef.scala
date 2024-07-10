// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS

object UseShadowRef {
  def hook[A]: CustomHook[A, NonEmptyRef.Get[A]] =
    CustomHook[A]
      .useRefBy(identity) // current
      .useEffectBy: (value, currentRef) =>
        currentRef.set(value)
      .buildReturning: (_, currentRef) =>
        currentRef

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Keeps a value in a ref. Useful for effectful get from a stable callback.
       */
      final def useShadowRef[A](value: Ctx => A)(using
        step: Step
      ): step.Next[NonEmptyRef.Get[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(value(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Keeps a value in a ref. Useful for effectful get from a stable callback.
       */
      def useShadowRef[A](value: CtxFn[A])(using step: Step): step.Next[NonEmptyRef.Get[A]] =
        useShadowRef(step.squash(value)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtShadowRef1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtShadowRef2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
