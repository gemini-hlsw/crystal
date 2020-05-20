package crystal.react

import implicits._
import cats.effect._
import cats.implicits._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.{ Ref => _, _ }
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger

import scala.concurrent.duration.FiniteDuration
import crystal.View
import crystal.data._
import crystal.data.react.implicits._

object StreamRendererMod {

  type Props[F[_], A] = View[F, Pot[A]] => VdomNode

  type State[A]           = Pot[A]
  type Component[F[_], A] =
    CtorType.Props[Props[F, A], UnmountedWithRoot[
      Props[F, A],
      _,
      _,
      _
    ]]

  def build[F[_]: ConcurrentEffect: Timer: Logger, A](
    stream:       fs2.Stream[F, A],
    holdAfterMod: Option[FiniteDuration] = None
  )(implicit
    reuse:        Reusability[A], // Used to derive Reusability[State[A]]
    renderReuse:  Reusability[Props[F, A]] = Reusability
      .always[Props[F, A]] // We assume rendering function doesn't change, but can be overriden.
  ): Component[F, A] = {
    class Backend($ : BackendScope[Props[F, A], State[A]])
        extends StreamRendererBackend[F, A](stream) {

      val hold: Hold[F, Pot[A]] =
        Hold($.setStateIn[F], holdAfterMod)
          .unsafeRunSync() // We cannot initialize the Backend effectfully, so we do this.

      override protected val directSetState: Pot[A] => F[Unit] = $.setStateIn[F]

      override protected lazy val streamSetState: Pot[A] => F[Unit] = hold.set

      def render(
        props: Props[F, A],
        state: Pot[A]
      ): VdomNode =
        props(
          View[F, Pot[A]](
            state,
            f => hold.enable.flatMap(_ => $.modStateIn[F](f))
          )
        )
    }

    ScalaComponent
      .builder[Props[F, A]]
      .initialState(Pot.pending[A])
      .renderBackend[Backend]
      .componentDidMount(_.backend.startUpdates)
      .componentWillUnmount(_.backend.stopUpdates)
      .configure(Reusability.shouldComponentUpdate)
      .build
  }
}
