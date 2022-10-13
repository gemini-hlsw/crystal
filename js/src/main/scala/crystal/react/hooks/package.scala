// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react

import cats.effect.Fiber
import cats.effect.Resource
import crystal.react.reuse._
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.DefaultEffects.{Async => DefaultA}

package object hooks
    extends UseSingleEffect.HooksApiExt
    with UseSerialState.HooksApiExt
    with UseStateCallback.HooksApiExt
    with UseStateView.HooksApiExt
    with UseStateViewWithReuse.HooksApiExt
    with UseSerialStateView.HooksApiExt
    with UseAsyncEffect.HooksApiExt
    with UseEffectResult.HooksApiExt
    with UseResource.HooksApiExt
    with UseStreamResource.HooksApiExt {
  type UnitFiber[F[_]] = Fiber[F, Throwable, Unit]
  type AsyncUnitFiber  = Fiber[DefaultA, Throwable, Unit]

  type StreamResource[A] = Resource[DefaultA, fs2.Stream[DefaultA, A]]

  protected[hooks] type NeverReuse = Reuse[Unit]
  protected[hooks] val NeverReuse: NeverReuse = ().reuseNever
}

package hooks {
  protected[hooks] final case class WithDeps[D, A](deps: D, fromDeps: D => A)
}
