package crystal.react

import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import crystal._
import crystal.implicits._
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.Effect
import org.typelevel.log4cats.Logger

abstract class StreamRendererBackend[F[_]: Async: Effect.Dispatch: Logger, A](
  stream: fs2.Stream[F, A]
) {
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
      .runAsync

  def stopUpdates: Callback =
    cancelToken.map(_.runAsync).getOrEmpty
}
