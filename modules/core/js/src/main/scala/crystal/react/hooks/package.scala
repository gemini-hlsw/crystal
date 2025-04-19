// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Fiber
import cats.effect.Resource
import cats.syntax.all.*
import crystal.Pot
import crystal.react.reuse.*
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

export UseSingleEffect.useSingleEffect, UseSerialState.useSerialState,
  UseStateCallback.useStateCallback, UseShadowRef.useShadowRef, UseStateView.useStateView,
  UseStateViewWithReuse.useStateViewWithReuse, UseSerialStateView.useSerialStateView,
  UseAsyncEffect.{useAsyncEffect, useAsyncEffectOnMount, useAsyncEffectWithDeps}, UseEffectResult.{
  useEffectKeepResultOnMount,
  useEffectKeepResultWhenDepsReady,
  useEffectKeepResultWhenDepsReadyOrChange,
  useEffectKeepResultWithDeps,
  useEffectResultOnMount,
  useEffectResultWhenDepsReady,
  useEffectResultWhenDepsReadyOrChange,
  useEffectResultWithDeps
}, UseResource.{useResource, useResourceOnMount}, UseThrottlingStateView.useThrottlingStateView,
  UseEffectStreamResource.{
  useEffectStream,
  useEffectStreamOnMount,
  useEffectStreamResource,
  useEffectStreamResourceOnMount,
  useEffectStreamResourceWhenDepsReady,
  useEffectStreamResourceWithDeps,
  useEffectStreamWhenDepsReady,
  useEffectStreamWithDeps
}, UseStreamResource.{
  useStream,
  useStreamOnMount,
  useStreamResource,
  useStreamResourceOnMount,
  useStreamResourceView,
  useStreamResourceViewOnMount,
  useStreamResourceViewWithReuse,
  useStreamResourceViewWithReuseOnMount,
  useStreamView,
  useStreamViewOnMount,
  useStreamViewWithReuse,
  useStreamViewWithReuseOnMount
}, UseEffectWhenDepsReady.{
  useAsyncEffectWhenDepsReady,
  useAsyncEffectWhenDepsReadyOrChange,
  useEffectWhenDepsReady,
  useEffectWhenDepsReadyOrChange
}

type UnitFiber[F[_]] = Fiber[F, Throwable, Unit]

type StreamResource[A] = Resource[DefaultA, fs2.Stream[DefaultA, A]]

protected[hooks] type NeverReuse = Reuse[Unit]
protected[hooks] val NeverReuse: NeverReuse = ().reuseNever

// *** The rest is to support builder-style hooks *** //

export UseSingleEffect.syntax.*, UseSerialState.syntax.*, UseStateCallback.syntax.*,
  UseStateView.syntax.*, UseStateViewWithReuse.syntax.*, UseSerialStateView.syntax.*,
  UseAsyncEffect.syntax.*, UseEffectResult.syntax.*, UseResource.syntax.*,
  UseStreamResource.syntax.*, UseEffectWhenDepsReady.syntax.*, UseEffectStreamResource.syntax.*,
  UseShadowRef.syntax.*, UseThrottlingStateView.syntax.*

protected[hooks] case class WithDeps[D, A](deps: D, fromDeps: D => A)

protected[hooks] enum WithPotDeps[D, A, R](
  val deps:       Pot[D],
  val fromDeps:   D => A,
  val reuseValue: Option[R]
):
  case WhenReady[D, A](override val deps: Pot[D], override val fromDeps: D => A)
      extends WithPotDeps[D, A, Unit](deps, fromDeps, deps.toOption.void)
  case WhenReadyOrChange[D, A](override val deps: Pot[D], override val fromDeps: D => A)
      extends WithPotDeps[D, A, D](deps, fromDeps, deps.toOption)
