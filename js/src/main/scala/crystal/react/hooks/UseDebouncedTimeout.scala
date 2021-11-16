package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.Ref
import cats.syntax.all._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

import scala.concurrent.duration.FiniteDuration

/** Given a `timeout`, this hooks provide a `TimeoutHandle` which can be used to submit an effect,
  * which will be run after the `timeout`. If a new effect is submitted before the `timeout`, the
  * previous one will be canceled (whether it already started running or not).
  *
  * A submitted effect can be explicitly canceled.
  */
object UseDebouncedTimeout {

  // Changing duration while component is mounted is not supported.
  val hook = CustomHook[FiniteDuration]
    .useMemoBy(_ => ())(duration =>
      _ =>
        new TimeoutHandle(
          duration,
          Ref.unsafe[DefaultA, Option[Deferred[DefaultA, TimeoutHandleLatch[DefaultA]]]](none)
        )
    )
    .useEffectBy((_, timeoutHandle) => Callback(timeoutHandle.cancel)) // Cleanup on unmount
    .buildReturning((_, timeoutHandle) => timeoutHandle)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      final def useDebouncedTimeout(timeout: FiniteDuration)(implicit
        step:                                Step
      ): step.Next[Reusable[TimeoutHandle[DefaultA]]] =
        useDebouncedTimeoutBy(_ => timeout)

      final def useDebouncedTimeoutBy(timeout: Ctx => FiniteDuration)(implicit
        step:                                  Step
      ): step.Next[Reusable[TimeoutHandle[DefaultA]]] =
        api.customBy(ctx => hook(timeout(ctx)))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      def useDebouncedTimeoutBy(timeout: CtxFn[FiniteDuration])(implicit
        step:                            Step
      ): step.Next[Reusable[TimeoutHandle[DefaultA]]] =
        useDebouncedTimeoutBy(step.squash(timeout)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtDebouncedTimeout1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtDebouncedTimeout2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                          CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
