// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.{Sync => DefaultS}

import scala.collection.immutable.Queue

object UseStateCallback {
  def hook[A] =
    CustomHook[Hooks.UseState[A]]
      .useRef(Queue.empty[A => DefaultS[Unit]])
      // Credit to japgolly for this implementation; this is copied from StateSnapshot.
      .useEffectBy { (state, delayedCallbacks) =>
        val cbs = delayedCallbacks.value
        if (cbs.isEmpty)
          DefaultS.empty
        else
          delayedCallbacks.set(Queue.empty) >>
            DefaultS.runAll(cbs.toList.map(_(state.value)): _*)
      }
      .buildReturning((_, delayedCallbacks) =>
        (cb: A => DefaultS[Unit]) => delayedCallbacks.mod(_.enqueue(cb))
      )

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Given a state, allows registering callbacks which are triggered when the state changes.
       */
      final def useStateCallback[A](state: => Hooks.UseState[A])(implicit
        step:                              Step
      ): step.Next[(A => DefaultS[Unit]) => DefaultS[Unit]] =
        useStateCallbackBy(_ => state)

      /**
       * Given a state, allows registering callbacks which are triggered when the state changes.
       */
      final def useStateCallbackBy[A](state: Ctx => Hooks.UseState[A])(implicit
        step:                                Step
      ): step.Next[(A => DefaultS[Unit]) => DefaultS[Unit]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(state(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Given a state, allows registering callbacks which are triggered when the state changes.
       */
      def useStateCallbackBy[A](state: CtxFn[Hooks.UseState[A]])(implicit
        step:                          Step
      ): step.Next[(A => DefaultS[Unit]) => DefaultS[Unit]] =
        useStateCallbackBy(step.squash(state)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtDelayedCallback1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtDelayedCallback2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                         CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
