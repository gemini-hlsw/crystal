package crystal.react

import implicits._
import cats.effect._
import cats.implicits._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger

import scala.scalajs.js
import crystal.data._
import crystal.data.implicits._
import crystal.data.react.implicits._

object StreamRenderer {
  type Props[A]     = Pot[A] => VdomNode
  type Component[A] =
    CtorType.Props[Props[A], UnmountedWithRoot[Props[A], _, _, _]]

  type State[A] = Pot[A]

  def build[F[_]: ConcurrentEffect: Logger, A](
    stream:      fs2.Stream[F, A],
    reusability: Reusability[A] = Reusability.by_==[A],
    key:         js.UndefOr[js.Any] = js.undefined
  ): Component[A] = {
    implicit val propsReuse: Reusability[Props[A]] =
      Reusability.byRef // Should this be Reusable[Props[A]]?
    implicit val aReuse: Reusability[A] = reusability

    class Backend($ : BackendScope[Props[A], State[A]]) {

      private var cancelToken: Option[CancelToken[F]] = None

      private val evalCancellable: SyncIO[CancelToken[F]] =
        ConcurrentEffect[F].runCancelable(
          stream
            .evalMap(v => $.setStateIn[F](v.ready))
            .compile
            .drain
        )(
          _.swap.toOption.foldMap(t =>
            $.setStateIn[IO](Error(t)) >>
              Effect[F].toIO(Logger[F].error(t)("[StreamRenderer] Error on stream"))
          )
        )

      def startUpdates =
        Callback {
          cancelToken = Some(evalCancellable.unsafeRunSync())
        }

      def stopUpdates =
        Callback { // Cancellation must be async. Is there a more elegant way of doing this?
          cancelToken.foreach(token => Effect[F].toIO(token).unsafeRunAsyncAndForget())
        }

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
      .withRawProp("key", key)
  }
}
