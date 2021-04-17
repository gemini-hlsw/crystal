package crystal.react

import crystal._
import crystal.implicits._
import cats.effect._
import cats.syntax.all._
import japgolly.scalajs.react._
import org.typelevel.log4cats.Logger
import cats.effect.std.Dispatcher
import scala.concurrent.Future

abstract class StreamRendererBackend[F[_]: Async: Logger, A](stream: fs2.Stream[F, A]) {
  type CancelToken = () => Future[Unit]
  private var cancelToken: Option[CancelToken] = None

  protected val directSetState: Pot[A] => F[Unit]

  protected lazy val streamSetState: Pot[A] => F[Unit] = directSetState

  // TODO Should be private
  lazy val evalCancellable: F[CancelToken] =
    Dispatcher[F]
      .use { dispatcher =>
        val fa: F[Unit] = stream
          .evalMap(v => streamSetState(v.ready))
          .compile
          .drain
        val ct          = dispatcher.unsafeRunCancelable(fa)
        cancelToken = ct.some
        ct.pure[F]
      }
      .handleErrorWith {
        // This is probably not right
        case t =>
          directSetState(Error(t)) >> Logger[F].error(t)("[StreamRenderer] Error on stream") >> (
            () => Future.unit
          ).pure[F]
      }

  def startUpdates =
    Callback.log("Start the evaluation")

  def stopUpdates: Callback =
    Callback(cancelToken.map(_()))
}
