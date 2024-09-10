// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.kernel.Monoid
import cats.syntax.all.*
import crystal.Pot
import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks.UseEffectArg
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

object UseEffectWhenDepsReady:

  def hook[D, A: UseEffectArg: Monoid, R: Reusability] =
    CustomHook[WithPotDeps[D, A, R]]
      .useEffectWithDepsBy(props => props.reuseValue): props =>
        _ => props.deps.toOption.map(props.fromDeps).orEmpty
      .build

  def asyncHook[G, D, R: Reusability](using EffectWithCleanup[G, DefaultA]) =
    CustomHook[WithPotDeps[D, G, R]]
      .useAsyncEffectWithDepsBy(props => props.reuseValue): props =>
        _ => props.deps.toOption.map(props.fromDeps(_).normalize).orEmpty
      .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Effect that runs whenever `Pot` dependencies transition into a `Ready` state (but not when
       * they change once `Ready`). For multiple dependencies, use `(par1, par2, ...).tupled`.
       * Dependencies are passed unpacked to the effect bulding function.
       */
      final def useEffectWhenDepsReadyBy[D, A: UseEffectArg: Monoid](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => A)(using
        step: Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook[D, A, Unit]
          hookInstance(WithPotDeps.WhenReady(deps(ctx), effect(ctx)))
        }

      /**
       * Effect that runs whenever `Pot` dependencies transition into a `Ready` state (but not when
       * they change once `Ready`). For multiple dependencies, use `(par1, par2, ...).tupled`.
       * Dependencies are passed unpacked to the effect bulding function.
       */
      final def useEffectWhenDepsReady[D, A: UseEffectArg: Monoid](
        deps: => Pot[D]
      )(effect: D => A)(using
        step: Step
      ): step.Self =
        useEffectWhenDepsReadyBy(_ => deps)(_ => effect)

      /**
       * Effect that runs when `Pot` dependencies transition into a `Ready` state or change once
       * `Ready`. For multiple dependencies, use `(par1, par2, ...).tupled`. Dependencies are passed
       * unpacked to the effect bulding function.
       */
      final def useEffectWhenDepsReadyOrChangeBy[D: Reusability, A: UseEffectArg: Monoid](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => A)(using
        step: Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook[D, A, D]
          hookInstance(WithPotDeps.WhenReadyOrChange(deps(ctx), effect(ctx)))
        }

      /**
       * Effect that runs when `Pot` dependencies transition into a `Ready` state or change once
       * `Ready`. For multiple dependencies, use `(par1, par2, ...).tupled`. Dependencies are passed
       * unpacked to the effect bulding function.
       */
      final def useEffectWhenDepsReadyOrChangeBy[D: Reusability, A: UseEffectArg: Monoid](
        deps: => Pot[D]
      )(effect: D => A)(using
        step: Step
      ): step.Self =
        useEffectWhenDepsReadyOrChangeBy(_ => deps)(_ => effect)

      /**
       * Async effect that runs whenever `Pot` dependencies transition into a `Ready` state (but not
       * when they change once `Ready`) and returns a cleanup callback. For multiple dependencies,
       * use `(par1, par2, ...).tupled`. Dependencies are passed unpacked to the effect bulding
       * function.
       */
      final def useAsyncEffectWhenDepsReady[G, D](deps: => Pot[D])(effect: D => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWhenDepsReadyBy(_ => deps)(_ => effect)

      /**
       * Async effect that runs whenever `Pot` dependencies transition into a `Ready` state (but not
       * when they change once `Ready`) and returns a cleanup callback. For multiple dependencies,
       * use `(par1, par2, ...).tupled`. Dependencies are passed unpacked to the effect bulding
       * function.
       */
      final def useAsyncEffectWhenDepsReadyBy[G, D](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = asyncHook[G, D, Unit]
          hookInstance(WithPotDeps.WhenReady(deps(ctx), effect(ctx)))
        }

      /**
       * Async effect that runs when `Pot` dependencies transition into a `Ready` state or change
       * once `Ready` and returns a cleanup callback. For multiple dependencies, use `(par1, par2,
       * ...).tupled`. Dependencies are passed unpacked to the effect bulding function.
       */
      final def useAsyncEffectWhenDepsReadyOrChange[G, D: Reusability](
        deps: => Pot[D]
      )(effect: D => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWhenDepsReadyOrChangeBy(_ => deps)(_ => effect)

      /**
       * Async effect that runs when `Pot` dependencies transition into a `Ready` state or change
       * once `Ready` and returns a cleanup callback. For multiple dependencies, use `(par1, par2,
       * ...).tupled`. Dependencies are passed unpacked to the effect bulding function.
       */
      final def useAsyncEffectWhenDepsReadyOrChangeBy[G, D: Reusability](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => G)(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = asyncHook[G, D, D]
          hookInstance(WithPotDeps.WhenReadyOrChange(deps(ctx), effect(ctx)))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Effect that runs whenever `Pot` dependencies transition into a `Ready` state (but not when
       * they change once `Ready`). For multiple dependencies, use `(par1, par2, ...).tupled`.
       * Dependencies are passed unpacked to the effect bulding function.
       */
      def useEffectWhenDepsReadyBy[D, A: UseEffectArg: Monoid](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => A])(using
        step: Step
      ): step.Self =
        useEffectWhenDepsReadyBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Effect that runs when `Pot` dependencies transition into a `Ready` state or change once
       * `Ready`. For multiple dependencies, use `(par1, par2, ...).tupled`. Dependencies are passed
       * unpacked to the effect bulding function.
       */
      def useEffectWhenDepsReadyOrChangeBy[D: Reusability, A: UseEffectArg: Monoid](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => A])(using
        step: Step
      ): step.Self =
        useEffectWhenDepsReadyOrChangeBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Async effect that runs whenever `Pot` dependencies transition into a `Ready` state (but not
       * when they change once `Ready`) and returns a cleanup callback. For multiple dependencies,
       * use `(par1, par2, ...).tupled`. Dependencies are passed unpacked to the effect bulding
       * function.
       */
      def useAsyncEffectWhenDepsReadyBy[G, D](deps: CtxFn[Pot[D]])(effect: CtxFn[D => G])(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWhenDepsReadyBy(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Async effect that runs when `Pot` dependencies transition into a `Ready` state or change
       * once `Ready` and returns a cleanup callback. For multiple dependencies, use `(par1, par2,
       * ...).tupled`. Dependencies are passed unpacked to the effect bulding function.
       */
      def useAsyncEffectWhenDepsReadyOrChangeBy[G, D: Reusability](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => G])(using
        step: Step,
        G:    EffectWithCleanup[G, DefaultA]
      ): step.Self =
        useAsyncEffectWhenDepsReadyOrChangeBy(step.squash(deps)(_))(step.squash(effect)(_))
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtEffectWhenDepsReady1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksEffectWhenDepsReady2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                          CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
