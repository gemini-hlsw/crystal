package crystal.react

import implicits._
import cats.effect._
import cats.implicits._
import japgolly.scalajs.react._
import _root_.io.chrisdavenport.log4cats.Logger

import crystal.data._
import crystal.data.implicits._
// import crystal.data.react.implicits._

abstract class StreamRendererBackend[F[_]: ConcurrentEffect: Logger, A](stream: fs2.Stream[F, A]) {
  private var cancelToken: Option[CancelToken[F]] = None

  protected val directSetState: Pot[A] => F[Unit]

  protected val streamSetState: Pot[A] => F[Unit] = directSetState

  private val evalCancellable: SyncIO[CancelToken[F]] =
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
