package crystal.react

import implicits._
import cats.effect._
import cats.implicits._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger
import _root_.io.chrisdavenport.log4cats.log4s.Log4sLogger

import scala.scalajs.js

object StreamRenderer {
  type Props[A]     = A => VdomNode
  type Component[A] =
    CtorType.Props[Props[A], UnmountedWithRoot[Props[A], _, _, _]]

  type State[A] = Option[A]

  implicit val logger = Log4sLogger.createLocal[IO]

  def build[F[_]: ConcurrentEffect, A](
    stream:      fs2.Stream[F, A],
    reusability: Reusability[A] = Reusability.by_==[A],
    key:         js.UndefOr[js.Any] = js.undefined
  ): Component[A] = {
    implicit val propsReuse: Reusability[Props[A]] = Reusability.byRef
    implicit val aReuse: Reusability[A]            = reusability

    class Backend($ : BackendScope[Props[A], State[A]]) {

      var cancelToken: Option[CancelToken[F]] = None

      val evalCancellable: SyncIO[CancelToken[F]] =
        ConcurrentEffect[F].runCancelable(
          stream
            .evalMap(v => $.setStateIn[F](Some(v)))
            .compile
            .drain
        )(
          _.swap.toOption.foldMap(e => Logger[IO].error(e)("[StreamRenderer] Error on stream"))
        )

      def startUpdates =
        Callback {
          cancelToken = Some(evalCancellable.unsafeRunSync())
        }

      def stopUpdates =
        Callback { // Cancellation must be async. Is there a more elegant way of doing this?
          cancelToken.foreach(token => Effect[F].toIO(token).unsafeRunAsyncAndForget())
        }

      def render(props: Props[A], state: Option[A]): VdomNode =
        state.fold(VdomNode(null))(props)
    }

    ScalaComponent
      .builder[Props[A]]("StreamRenderer")
      .initialState(Option.empty[A])
      .renderBackend[Backend]
      .componentDidMount(_.backend.startUpdates)
      .componentWillUnmount(_.backend.stopUpdates)
      .configure(Reusability.shouldComponentUpdate)
      .build
      .withRawProp("key", key)
  }
}
