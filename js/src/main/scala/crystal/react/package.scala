package crystal

import cats.effect.SyncIO
import crystal.react.implicits._
import crystal.react.reuse.Reuse
import cats.effect.Async
import cats.effect.std.Dispatcher
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA, Sync => DefaultS }
import org.typelevel.log4cats.Logger

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
    def apply[F[_], A[_], S]($ : StateAccess[DefaultS, DefaultA, S]): ViewF[SyncIO, S] =
      ViewF($.state.runNow(), (f, cb) => $.modStateInSyncIO(f, cb))
  }
}
