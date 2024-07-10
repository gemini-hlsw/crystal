// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react

import cats.arrow.FunctionK
import cats.~>
import crystal.*
import crystal.react.reuse.Reuse
import crystal.react.syntax.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS
import japgolly.scalajs.react.util.Effect.UnsafeSync
import japgolly.scalajs.react.vdom.VdomNode

import scala.reflect.ClassTag

type SetState[F[_], A] = A => F[Unit]
type ModState[F[_], A] = (A => A) => F[Unit]

type ContextComponent[S, B] =
  ScalaComponent[
    Reuse[VdomNode],
    S,
    B,
    CtorType.Props
  ]

type StateComponent[S, B] =
  ScalaComponent[Reuse[View[S] => VdomNode], S, B, CtorType.Props]

type View[A]     = ViewF[DefaultS, A]
type ViewOpt[A]  = ViewOptF[DefaultS, A]
type ViewList[A] = ViewListF[DefaultS, A]

type ReuseViewF[F[_], A]     = Reuse[ViewF[F, A]]
type ReuseViewOptF[F[_], A]  = Reuse[ViewOptF[F, A]]
type ReuseViewListF[F[_], A] = Reuse[ViewListF[F, A]]

type ReuseView[A]     = Reuse[View[A]]
type ReuseViewOpt[A]  = Reuse[ViewOpt[A]]
type ReuseViewList[A] = Reuse[ViewList[A]]

export crystal.react.syntax.all.*, crystal.react.syntax.all.given

val syncToAsync: DefaultS ~> DefaultA = new FunctionK[DefaultS, DefaultA] { self =>
  def apply[A](fa: DefaultS[A]): DefaultA[A] = fa.toAsync
}

def View[A](
  value: A,
  modCB: (A => A, (Option[A], A) => DefaultS[Unit]) => DefaultS[Unit]
): View[A] = ViewF[DefaultS, A](value, modCB)

// def fromState = new FromStateView

// class FromStateView {
//   def apply[S]($ : StateAccess[DefaultS, DefaultA, S])(using
//     dispatch: UnsafeSync[DefaultS]
//   ): View[S] =
//     View[S](
//       dispatch.runSync($.state),
//       (f, cb) => $.modState(f, $.state.flatMap(cb))
//     )
// }

object ReuseView {
  inline def apply[A: ClassTag: Reusability](
    value: A,
    modCB: (A => A, (Option[A], A) => DefaultS[Unit]) => DefaultS[Unit]
  ): ReuseView[A] =
    View(value, modCB).reuseByValue

  def fromState = new FromStateReuseView
}

class FromStateReuseView {
  def apply[S: ClassTag: Reusability]($ : StateAccess[DefaultS, DefaultA, S])(using
    dispatch: UnsafeSync[DefaultS]
  ): ReuseView[S] =
    ReuseView[S](
      dispatch.runSync($.state),
      (f, cb) => $.modState(f, $.state.flatMap(cb))
    )
}
