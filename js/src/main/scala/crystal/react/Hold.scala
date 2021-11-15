package crystal.react

import cats.effect.Async
import cats.effect.Ref
import cats.effect.Sync
import cats.effect.Temporal
import cats.effect.implicits._
import cats.syntax.all._
import crystal.implicits._
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

import scala.concurrent.duration.FiniteDuration

/** Encapsulates an effectful `setter`. When `enable` is called, calls to `setter` will be delayed
  * for `duration`. Each call to `enable` resets the internal timer, i.e: `duration` is guaranteed
  * to have elapsed since last call to `enable` before calling `setter`.
  */
class Hold[F[_]: Async, A](
  setter:      A => F[Unit],
  duration:    Option[FiniteDuration],
  cancelToken: Ref[F, Option[F[Unit]]],
  buffer:      Ref[F, Option[A]]
) {
  def set(a: A): F[Unit] =
    cancelToken.get.flatMap(
      _.fold(setter(a))(_ => buffer.set(a.some))
    )

  private val restart: Option[F[Unit]] =
    duration.map { d =>
      for {
        _ <- Temporal[F].sleep(d)
        _ <- cancelToken.set(none)
        b <- buffer.getAndSet(none)
        _ <- b.map(setter).orUnit
      } yield ()
    }

  val enable: F[Unit] =
    (cancelToken.getAndSet(none).flatMap(_.orUnit)).uncancelable >>
      restart.map(_.start.flatMap(fiber => cancelToken.set(fiber.cancel.some))).orUnit
}

object Hold {
  def apply[F[_]: Async, A](
    setter:            A => F[Unit],
    duration:          Option[FiniteDuration]
  )(implicit DefaultS: Sync[DefaultS]): DefaultS[Hold[F, A]] =
    for {
      cancelToken <- Ref.in[DefaultS, F, Option[F[Unit]]](none)
      buffer      <- Ref.in[DefaultS, F, Option[A]](none)
    } yield new Hold(setter, duration, cancelToken, buffer)
}
