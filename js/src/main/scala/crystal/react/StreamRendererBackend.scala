package crystal.react

import crystal._
import crystal.implicits._
import crystal.react.implicits._
import cats.effect._
import cats.syntax.all._
import japgolly.scalajs.react._
import org.typelevel.log4cats.Logger

abstract class StreamRendererBackend[F[_]: ConcurrentEffect: Logger, A](stream: fs2.Stream[F, A]) {
  private var cancelToken: Option[CancelToken[F]] = None

  protected val directSetState: Pot[A] => F[Unit]

  protected lazy val streamSetState: Pot[A] => F[Unit] = directSetState

  private lazy val evalCancellable: SyncIO[CancelToken[F]] =
    ConcurrentEffect[F].runCancelable(
      stream
        .evalMap(v => streamSetState(v.ready))
        .compile
        .drain
    )(
      _.swap.toOption.foldMap(t =>
        Effect[F].toIO(
          directSetState(Error(t)) >>
            Logger[F].error(t)("[StreamRenderer] Error on stream")
        )
      )
    )

  def startUpdates =
    evalCancellable.toCB >>= ((token: CancelToken[F]) =>
      Callback.lift(() => cancelToken = token.some)
    )

  def stopUpdates =
    cancelToken.map(token => Effect[F].runAsync(token)(_ => IO.unit).toCB).getOrEmpty
}
