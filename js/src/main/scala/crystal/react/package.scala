package crystal

import crystal.react.implicits._
import cats.effect.Async
import cats.effect.ConcurrentEffect
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.typelevel.log4cats.Logger

package object react {
  type SetState[F[_], A] = A => F[Unit]
  type ModState[F[_], A] = (A => A) => F[Unit]

  type StateComponent[S] =
    ScalaComponent[
      Unit,
      S,
      Unit,
      CtorType.Nullary
    ]

  implicit def renderComponent[S](component: crystal.react.StateComponent[S]): VdomNode =
    component()

  implicit class StreamOps[F[_], A](private val s: fs2.Stream[F, A]) {
    def render(implicit
      ce:     ConcurrentEffect[F],
      logger: Logger[F],
      reuse:  Reusability[A]
    ): StreamRenderer.Component[A] =
      StreamRenderer.build(s)
  }
}

package react {
  import japgolly.scalajs.react.component.builder.Lifecycle.StateRW

  class FromStateViewF[F[_]]() {
    def apply[S](
      $              : StateAccess[CallbackTo, S]
    )(implicit async: Async[F]): ViewF[F, S] =
      ViewF($.state.runNow(), $.modStateIn[F])

    def apply[S](
      $              : StateRW[_, S, _]
    )(implicit async: Async[F]): ViewF[F, S] =
      ViewF($.state, $.modStateIn[F])
  }
}
