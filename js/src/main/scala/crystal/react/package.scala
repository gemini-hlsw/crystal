package crystal

import crystal.react.implicits._
import cats.effect.Async
import cats.effect.std.Dispatcher
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.typelevel.log4cats.Logger
import crystal.react.reuse.Reuse
import cats.effect.SyncIO

package object react {
  type SetState[F[_], A] = A => F[Unit]
  type ModState[F[_], A] = (A => A) => F[Unit]

  type ContextComponent[S] =
    ScalaComponent[
      Reuse[VdomNode],
      S,
      _,
      CtorType.Props
    ]

  type StateComponent[S] =
    ScalaComponent[
      Reuse[ViewF[SyncIO, S] => VdomNode],
      S,
      _,
      CtorType.Props
    ]

  implicit class StreamOps[F[_], A](private val s: fs2.Stream[F, A]) {
    def render(implicit
      async:      Async[F],
      dispatcher: Dispatcher[F],
      logger:     Logger[F],
      reuse:      Reusability[A]
    ): StreamRenderer.Component[A] =
      StreamRenderer.build(s)
  }
}

package react {
  import cats.effect.SyncIO

  class FromStateViewSyncIO {
    def apply[S]($ : StateAccess[CallbackTo, S]): ViewF[SyncIO, S] =
      ViewF($.state.runNow(), (f, cb) => $.modStateInSyncIO(f, cb))
  }
}
