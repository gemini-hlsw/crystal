package crystal.react

import crystal.implicits._
import cats.effect._
import cats.syntax.all._
import cats.effect.implicits._
import scala.concurrent.duration.FiniteDuration
import cats.effect.{ Ref, Temporal }
import scala.concurrent.Future
import cats.effect.std.Dispatcher

/** Encapsulates an effectful `setter`. When `enable` is called, calls to
  * `setter` will be delayed for `duration`. Each call to `enable` resets
  * the internal timer, i.e: `duration` is guaranteed to have elapsed
  * since last call to `enable` before calling `setter`.
  */
class Hold[F[_]: Async, A](
  setter:      A => F[Unit],
  duration:    Option[FiniteDuration],
  cancelToken: Ref[F, Option[() => Future[Unit]]],
  buffer:      Ref[F, Option[A]]
) {
  def set(a: A): F[Unit] =
    cancelToken.get.flatMap(
      _.fold(setter(a))(_ => buffer.set(a.some))
    )

  private val restart: Option[F[Unit]] =
    duration.map { d =>
      for {
        _ <- (cancelToken.getAndSet(None).map(_.getOrElse(() => Future.unit))).uncancelable
        _ <- Temporal[F].sleep(d)
        _ <- cancelToken.set(None)
        b <- buffer.getAndSet(None)
        _ <- b.map(set).orUnit
      } yield ()
    }

  val enable: F[Unit] =
    restart.map { r =>
      Dispatcher[F].use { dispatcher =>
        cancelToken.set(dispatcher.unsafeRunCancelable(r).some)
      }
    }.orUnit
}

object Hold {
  def apply[F[_]: Async, A](
    setter:   A => F[Unit],
    duration: Option[FiniteDuration]
  ): SyncIO[Hold[F, A]] =
    for {
      cancelToken <- Ref.in[SyncIO, F, Option[() => Future[Unit]]](None)
      buffer      <- Ref.in[SyncIO, F, Option[A]](None)
    } yield new Hold(setter, duration, cancelToken, buffer)
}
