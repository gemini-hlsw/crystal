// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.syntax.option.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS

object UsePrevious {
  def hook[A]: CustomHook[A, NonEmptyRef.Get[Option[A]]] =
    CustomHook[A]
      .useRefBy(identity) // current
      .useRef(none[A])    // previous
      .useEffectBy: (value, currentRef, previousRef) =>
        if (currentRef.value == value)
          DefaultS.empty
        else
          previousRef.set(currentRef.value.some) >>
            currentRef.set(value)
      .buildReturning: (_, _, previousRef) =>
        previousRef

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Given a value, remembers the previous value.
       */
      final def usePrevious[A](value: Ctx => A)(using
        step: Step
      ): step.Next[NonEmptyRef.Get[Option[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(value(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Given a value, remembers the previous value.
       */
      def usePrevious[A](value: CtxFn[A])(using
        step: Step
      ): step.Next[NonEmptyRef.Get[Option[A]]] =
        usePrevious(step.squash(value)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtPrevious1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtPrevious2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
