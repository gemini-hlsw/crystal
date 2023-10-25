// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.kernel.Monoid
import cats.syntax.all.*
import crystal.Pot
import crystal.react.*
import crystal.react.syntax.pot.given
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks.UseEffectArg
import japgolly.scalajs.react.util.DefaultEffects.{Async => DefaultA}

object UseEffectWhenDepsReady:
  def hook[D, A: UseEffectArg: Monoid] =
    CustomHook[WithPotDeps[D, A]]
      .useEffectWithDepsBy(props => props.deps.void)(props =>
        _ => props.deps.toOption.map(props.fromDeps).orEmpty
      )
      .build

  def asyncHook[D] =
    CustomHook[WithPotDeps[D, DefaultA[DefaultA[Unit]]]]
      .useAsyncEffectWithDepsBy(props => props.deps.void)(props =>
        _ => props.deps.toOption.map(props.fromDeps).orEmpty
      )
      .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Effect that runs when `Pot` dependencies transition into a `Ready` state. For multiple
       * dependencies, use `(par1, par2, ...).tupled`. Dependencies are passed unpacked to the
       * effect bulding function. Returning a cleanup callback is supported, but when using an async
       * effect with cleanup use `useAsyncEffectWhenDepsReady` instead.
       */
      final def useEffectWhenDepsReady[D, A: UseEffectArg: Monoid](
        deps: => Pot[D]
      )(effect: D => A)(using
        step: Step
      ): step.Self =
        useEffectWhenDepsReady(_ => deps)(_ => effect)

      /**
       * Effect that runs when `Pot` dependencies transition into a `Ready` state. For multiple
       * dependencies, use `(par1, par2, ...).tupled`. Dependencies are passed unpacked to the
       * effect bulding function. Returning a cleanup callback is supported, but when using an async
       * effect with cleanup use `useAsyncEffectWhenDepsReady` instead.
       */
      final def useEffectWhenDepsReady[D, A: UseEffectArg: Monoid](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => A)(using
        step: Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance(WithPotDeps(deps(ctx), effect(ctx)))
        }

      /**
       * Async effect that runs when `Pot` dependencies transition into a `Ready` state and returns
       * a cleanup callback. For multiple dependencies, use `(par1, par2, ...).tupled`. Dependencies
       * are passed unpacked to the effect bulding function.
       */
      final def useAsyncEffectWhenDepsReady[D](
        deps: => Pot[D]
      )(effect: D => DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self =
        useAsyncEffectWhenDepsReady(_ => deps)(_ => effect)

      /**
       * Async effect that runs when `Pot` dependencies transition into a `Ready` state and returns
       * a cleanup callback. For multiple dependencies, use `(par1, par2, ...).tupled`. Dependencies
       * are passed unpacked to the effect bulding function.
       */
      final def useAsyncEffectWhenDepsReady[D](
        deps: Ctx => Pot[D]
      )(effect: Ctx => D => DefaultA[DefaultA[Unit]])(using
        step: Step
      ): step.Self =
        api.customBy { ctx =>
          val hookInstance = asyncHook[D]
          hookInstance(WithPotDeps(deps(ctx), effect(ctx)))
        }
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Effect that runs when `Pot` dependencies transition into a `Ready` state. For multiple
       * dependencies, use `(par1, par2, ...).tupled`. Dependencies are passed unpacked to the
       * effect bulding function. Returning a cleanup callback is supported, but when using an async
       * effect with cleanup use `useAsyncEffectWhenDepsReady` instead.
       */
      def useEffectWhenDepsReady[D, A: UseEffectArg: Monoid](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => A])(using
        step: Step
      ): step.Self =
        useEffectWhenDepsReady(step.squash(deps)(_))(step.squash(effect)(_))

      /**
       * Async effect that runs when `Pot` dependencies transition into a `Ready` state and returns
       * a cleanup callback. For multiple dependencies, use `(par1, par2, ...).tupled`. Dependencies
       * are passed unpacked to the effect bulding function.
       */
      def useAsyncEffectWhenDepsReady[D](
        deps: CtxFn[Pot[D]]
      )(effect: CtxFn[D => DefaultA[DefaultA[Unit]]])(using
        step: Step
      ): step.Self =
        useAsyncEffectWhenDepsReady(step.squash(deps)(_))(step.squash(effect)(_))
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
