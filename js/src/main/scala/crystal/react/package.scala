package crystal

import cats.arrow.FunctionK
import cats.effect.Async
import cats.~>
import crystal.react.implicits._
import crystal.react.reuse.Reuse
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }
import japgolly.scalajs.react.util.Effect
import japgolly.scalajs.react.util.Effect.UnsafeSync
import japgolly.scalajs.react.vdom.VdomNode
import org.typelevel.log4cats.Logger

import scala.reflect.ClassTag

package object react {
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

  implicit class StreamOps[F[_], A](private val s: fs2.Stream[F, A]) extends AnyVal {
    def render(implicit
      async:      Async[F],
      dispatcher: Effect.Dispatch[F],
      logger:     Logger[F],
      reuse:      Reusability[A]
    ): StreamRenderer.Component[A] =
      StreamRenderer.build(s)
  }

  type View[A]    = ViewF[DefaultS, A]
  type ViewOpt[A] = ViewOptF[DefaultS, A]

  type ReuseViewF[F[_], A]    = Reuse[ViewF[F, A]]
  type ReuseViewOptF[F[_], A] = Reuse[ViewOptF[F, A]]

  type ReuseView[A]    = Reuse[View[A]]
  type ReuseViewOpt[A] = Reuse[ViewOpt[A]]

  val syncToAsync: DefaultS ~> DefaultA = new FunctionK[DefaultS, DefaultA] { self =>
    def apply[A](fa: DefaultS[A]): DefaultA[A] = fa.to[DefaultA]
  }

}

package react {
  object View {
    @inline
    def apply[A](
      value: A,
      modCB: ((A => A), A => DefaultS[Unit]) => DefaultS[Unit]
    ): View[A] = ViewF[DefaultS, A](value, modCB)
  }

  class FromStateView {
    def apply[S]($ : StateAccess[DefaultS, DefaultA, S])(implicit
      dispatch:      UnsafeSync[DefaultS]
    ): View[S] =
      View[S](
        dispatch.runSync($.state),
        (f, cb) => $.modState(f, $.state.flatMap(cb))
      )
  }

  object ReuseView {
    @inline
    def apply[A: ClassTag: Reusability](
      value: A,
      modCB: ((A => A), A => DefaultS[Unit]) => DefaultS[Unit]
    ): ReuseView[A] =
      Reuse.by(value)(View(value, modCB))
  }

  class FromStateReuseView {
    def apply[S: ClassTag: Reusability]($ : StateAccess[DefaultS, DefaultA, S])(implicit
      dispatch:                             UnsafeSync[DefaultS]
    ): ReuseView[S] =
      ReuseView[S](
        dispatch.runSync($.state),
        (f, cb) => $.modState(f, $.state.flatMap(cb))
      )
  }

}
