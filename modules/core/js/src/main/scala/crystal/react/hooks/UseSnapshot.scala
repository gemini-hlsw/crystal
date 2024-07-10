// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.syntax.option.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS

// Serial reusability?
case class UseSnapshot[A] private (
  private val ref: NonEmptyRef.Get[Option[A]],
  capture:         DefaultS[Unit]
):
  export ref.*

object UseSnapshot {
  def hook[A]: CustomHook[A, UseSnapshot[A]] =
    CustomHook[A]
      .useRefBy(identity) // current
      .useRef(none[A])    // snapshot
      .useEffectBy: (value, currentRef, _) =>
        currentRef.set(value)
      .useCallbackBy: (_, currentRef, snapshotRef) =>
        snapshotRef.set(currentRef.value)
      .buildReturning: (_, _, snapshotRef, capture) =>
        UseSnapshot(snapshotRef, capture)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       */
      final def useSnapshot[A](value: Ctx => A)(using step: Step): step.Next[UseSnapshot[A]] =
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
      def useSnapshot[A](value: CtxFn[A])(using step: Step): step.Next[UseSnapshot] =
        useSnapshot(step.squash(value)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtSnapshot1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtSnapshot2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
