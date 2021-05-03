package crystal.react

import crystal._
import crystal.implicits._
import crystal.react.implicits._
import cats.effect._
import cats.effect.syntax.all._
import cats.effect.std.Dispatcher
import cats.syntax.all._
import japgolly.scalajs.react._
import org.typelevel.log4cats.Logger

abstract class StreamRendererBackend[F[_]: Async: Dispatcher: Logger, A](stream: fs2.Stream[F, A]) {
  type CancelToken = F[Unit]
  private var cancelToken: Option[CancelToken] = None

  protected val directSetState: Pot[A] => F[Unit]

  protected lazy val streamSetState: Pot[A] => F[Unit] = directSetState

  def startUpdates: Callback =
    stream
      .evalMap(v => streamSetState(v.ready))
      .compile
      .drain
      .handleErrorWith { case t =>
        cancelToken = none
        directSetState(Error(t)) >> Logger[F].error(t)("[StreamRenderer] Error on stream")
      }
      .start
      .map(fiber => cancelToken = fiber.cancel.some)
      .runAsyncCB

  def stopUpdates: Callback =
    cancelToken.map(_.runAsyncCB).getOrEmpty
}
