package crystal.react

import cats.effect._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger
import crystal.react.implicits._
import crystal.data._
import crystal.data.react.implicits._

object StreamRenderer {
  type Props[A]     = Pot[A] => VdomNode
  type Component[A] =
    CtorType.Props[Props[A], UnmountedWithRoot[Props[A], _, _, _]]

  type State[A] = Pot[A]

  def build[F[_]: ConcurrentEffect: Logger, A](
    stream:      fs2.Stream[F, A]
  )(implicit
    reuse:       Reusability[A], // Used to derive Reusability[State[A]]
    renderReuse: Reusability[Props[A]] = Reusability
      .always[Props[A]] // We assume rendering function doesn't change, but can be overriden.
  ): Component[A] = {
    class Backend($ : BackendScope[Props[A], State[A]])
        extends StreamRendererBackend[F, A](stream) {

      override protected val directSetState: Pot[A] => F[Unit] = $.setStateIn[F]

      def render(props: Props[A], state: Pot[A]): VdomNode =
        props(state)
    }

    ScalaComponent
      .builder[Props[A]]
      .initialState(Pot.pending[A])
      .renderBackend[Backend]
      .componentDidMount(_.backend.startUpdates)
      .componentWillUnmount(_.backend.stopUpdates)
      .configure(Reusability.shouldComponentUpdate)
      .build
  }
}
