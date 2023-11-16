// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Fiber
import cats.effect.Resource
import crystal.Pot
import crystal.react.reuse.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.util.DefaultEffects.{Async => DefaultA}

export UseSingleEffect.syntax.*, UseSerialState.syntax.*, UseStateCallback.syntax.*,
  UseStateView.syntax.*, UseStateViewWithReuse.syntax.*, UseSerialStateView.syntax.*,
  UseAsyncEffect.syntax.*, UseEffectResult.syntax.*, UseResource.syntax.*,
  UseStreamResource.syntax.*, UseEffectWhenDepsReady.syntax.*

type UnitFiber[F[_]] = Fiber[F, Throwable, Unit]
type AsyncUnitFiber  = Fiber[DefaultA, Throwable, Unit]

type StreamResource[A] = Resource[DefaultA, fs2.Stream[DefaultA, A]]

protected[hooks] type NeverReuse = Reuse[Unit]
protected[hooks] val NeverReuse: NeverReuse = ().reuseNever

protected[hooks] final case class WithDeps[D, A](deps: D, fromDeps: D => A)

protected[hooks] final case class WithPotDeps[D, A](deps: Pot[D], fromDeps: D => A)
