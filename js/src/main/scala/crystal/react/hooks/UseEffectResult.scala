package crystal.react.hooks

import crystal.Pot
import crystal.implicits._
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.{Async => DefaultA}

object UseEffectResult {
  def hook[D: Reusability, A] = CustomHook[WithDeps[D, DefaultA[A]]]
    .useState(Pot.pending[A])
    .useEffectWithDepsBy((props, _) => props.deps)((_, state) => _ => state.setState(Pot.pending))
    .useEffectWithDepsBy((props, _) => props.deps)((props, state) =>
      deps =>
        (for {
          a <- props.fromDeps(deps)
          _ <- state.setStateAsync(a.ready)
        } yield ()).handleErrorWith(t => state.setStateAsync(Pot.error(t)))
    )
    .buildReturning((_, state) => state.value)

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultWithDeps[D: Reusability, A](
        deps:   => D
      )(effect: D => DefaultA[A])(implicit
        step:   Step
      ): step.Next[Pot[A]] =
        useEffectResultWithDepsBy(_ => deps)(_ => effect)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResult[A](effect: DefaultA[A])(implicit
        step:                              Step
      ): step.Next[Pot[A]] =
        useEffectResultBy(_ => effect)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMount[A](effect: DefaultA[A])(implicit
        step:                                     Step
      ): step.Next[Pot[A]] =
        useEffectResultOnMountBy(_ => effect)

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultWithDepsBy[D: Reusability, A](
        deps:   Ctx => D
      )(effect: Ctx => D => DefaultA[A])(implicit
        step:   Step
      ): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance(WithDeps(deps(ctx), effect(ctx)))
        }

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultBy[A](effect: Ctx => DefaultA[A])(implicit
        step:                                Step
      ): step.Next[Pot[A]] =
        useEffectResultWithDepsBy(_ => NeverReuse)(ctx => _ => effect(ctx))

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMountBy[A](effect: Ctx => DefaultA[A])(implicit
        step:                                       Step
      ): step.Next[Pot[A]] = // () has Reusability = always.
        useEffectResultWithDepsBy(_ => ())(ctx => _ => effect(ctx))
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      def useEffectResultWithDepsBy[D: Reusability, A](
        deps:   CtxFn[D]
      )(effect: CtxFn[D => DefaultA[A]])(implicit
        step:   Step
      ): step.Next[Pot[A]] =
        useEffectResultWithDepsBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResult[A](effect: CtxFn[DefaultA[A]])(implicit
        step:                              Step
      ): step.Next[Pot[A]] =
        useEffectResultBy(step.squash(effect)(_))

      /**
       * Runs an async effect and stores the result in a state, which is provided as a `Pot[A]`.
       */
      final def useEffectResultOnMountBy[A](effect: CtxFn[DefaultA[A]])(implicit
        step:                                       Step
      ): step.Next[Pot[A]] =
        useEffectResultOnMountBy(step.squash(effect)(_))
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

    implicit def hooksExtEffectResult1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtEffectResult2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object implicits extends HooksApiExt
}
