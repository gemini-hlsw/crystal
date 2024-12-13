// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.Pot
import crystal.react.ThrottlingView
import crystal.react.ViewThrottler
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook

import scala.concurrent.duration.FiniteDuration

object UseThrottlingStateView:
  /** Creates component state as a `ThrottlingView`. See `ViewThrottler[A]`. */
  final def useThrottlingStateView[A](
    input: (A, FiniteDuration)
  ): HookResult[Pot[ThrottlingView[A]]] =
    for
      view      <- useStateView(input._1)
      throttler <- useEffectResultOnMount(ViewThrottler[A](input._2))
    yield throttler.map(_.throttle(view))

  // *** The rest is to support builder-style hooks *** //

  private def hook[A]: CustomHook[(A, FiniteDuration), Pot[ThrottlingView[A]]] =
    CustomHook.fromHookResult(useThrottlingStateView(_))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a `ThrottlingView`. See `ViewThrottler[A]`. */
      final def useThrottlingStateView[A](initialValue: => A, timeout: FiniteDuration)(using
        step: Step
      ): step.Next[Pot[ThrottlingView[A]]] =
        useThrottlingStateViewBy(_ => (initialValue, timeout))

      /** Creates component state as a `ThrottlingView`. See `ViewThrottler[A]`. */
      final def useThrottlingStateViewBy[A](props: Ctx => (A, FiniteDuration))(using
        step: Step
      ): step.Next[Pot[ThrottlingView[A]]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(props(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state as a `ThrottlingView`. See `ViewThrottler[A]`. */
      def useThrottlingStateViewBy[A](props: CtxFn[(A, FiniteDuration)])(using
        step: Step
      ): step.Next[Pot[ThrottlingView[A]]] =
        useThrottlingStateViewBy(step.squash(props)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtThrottlingStateView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtThrottlingStateView2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[
      Ctx,
      CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
