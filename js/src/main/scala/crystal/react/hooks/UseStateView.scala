package crystal.react.hooks

import cats.syntax.all._
import crystal.react.View
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

import scala.collection.immutable.Queue
import scala.reflect.ClassTag

object UseStateView {
  def hook[A: Reusability: ClassTag]: CustomHook[A, View[A]] =
    CustomHook[A]
      .useStateWithReuseBy(initialValue => initialValue)
      .useRef(Queue.empty[A => DefaultS[Unit]])
      // Credit to japgolly for this implementation; this is copied from StateSnapshot.
      .useEffectBy { (_, state, delayedCallbacks) =>
        val cbs = delayedCallbacks.value
        if (cbs.isEmpty)
          DefaultS.empty
        else
          delayedCallbacks.set(Queue.empty) >>
            DefaultS.runAll(cbs.toList.map(_(state.value)): _*)
      }
      .buildReturning { (_, state, delayedCallbacks) =>
        View[A](
          state.value,
          (f, cb) => state.modState(f) >> delayedCallbacks.mod(_.enqueue(cb))
        )
      }

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a View */
      final def useStateView[A: Reusability: ClassTag](initialValue: => A)(implicit
        step:                                                        Step
      ): step.Next[View[A]] =
        useStateViewBy(_ => initialValue)

      /** Creates component state as a View */
      final def useStateViewBy[A: Reusability: ClassTag](initialValue: Ctx => A)(implicit
        step:                                                          Step
      ): step.Next[View[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[A]
          hookInstance(initialValue(ctx))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /** Creates component state as a View */
      def useStateViewBy[A: Reusability: ClassTag](initialValue: CtxFn[A])(implicit
        step:                                                    Step
      ): step.Next[View[A]] =
        useStateViewBy(step.squash(initialValue)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtStateView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStateView2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
