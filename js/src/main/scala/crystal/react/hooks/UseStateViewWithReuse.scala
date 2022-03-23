package crystal.react.hooks

import crystal.react.ReuseView
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

import scala.collection.immutable.Queue
import scala.reflect.ClassTag

object UseStateViewWithReuse {
  def hook[A: ClassTag: Reusability]: CustomHook[A, ReuseView[A]] =
    CustomHook[A]
      .useStateBy(initialValue => initialValue)
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
        ReuseView[A](
          state.value,
          (f, cb) => state.modState(f) >> delayedCallbacks.mod(_.enqueue(cb))
        )
      }

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /** Creates component state as a View */
      final def useStateViewWithReuse[A: ClassTag: Reusability](initialValue: => A)(implicit
        step:                                                                 Step
      ): step.Next[ReuseView[A]] =
        useStateViewWithReuseBy(_ => initialValue)

      /** Creates component state as a View */
      final def useStateViewWithReuseBy[A: ClassTag: Reusability](initialValue: Ctx => A)(implicit
        step:                                                                   Step
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
      def useStateViewWithReuseBy[A: ClassTag: Reusability](initialValue: CtxFn[A])(implicit
        step:                                                             Step
      ): step.Next[ReuseView[A]] =
        useStateViewWithReuseBy(step.squash(initialValue)(_))
    }
  }

  trait HooksApiExt {
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

  object implicits extends HooksApiExt
}
