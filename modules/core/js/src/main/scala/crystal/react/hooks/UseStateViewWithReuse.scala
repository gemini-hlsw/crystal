// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.react.*
import crystal.react.ReuseView
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook

import scala.reflect.ClassTag

object UseStateViewWithReuse:
  /** Creates component state as a View */
  final def useStateViewWithReuse[A: ClassTag: Reusability](
    initialValue: => A
  ): HookResult[ReuseView[A]] =
    useStateView(initialValue).map(_.reuseByValue)

  // *** The rest is to support builder-style hooks *** //

  private def hook[A: ClassTag: Reusability]: CustomHook[A, ReuseView[A]] =
    CustomHook.fromHookResult(useStateViewWithReuse(_))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a View */
      final def useStateViewWithReuse[A: ClassTag: Reusability](initialValue: => A)(using
        step: Step
      ): step.Next[ReuseView[A]] =
        useStateViewWithReuseBy(_ => initialValue)

      /** Creates component state as a View */
      final def useStateViewWithReuseBy[A: ClassTag: Reusability](initialValue: Ctx => A)(using
        step: Step
      ): step.Next[ReuseView[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(initialValue(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state as a View */
      def useStateViewWithReuseBy[A: ClassTag: Reusability](initialValue: CtxFn[A])(using
        step: Step
      ): step.Next[ReuseView[A]] =
        useStateViewWithReuseBy(step.squash(initialValue)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStateViewWithReuse1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStateViewWithReuse2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                            CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
