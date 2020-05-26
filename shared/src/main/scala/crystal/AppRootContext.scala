package crystal

import cats.FlatMap
import cats.implicits._
import cats.effect.SyncIO
import cats.effect.LiftIO
import cats.effect.concurrent.Ref
import cats.Functor

trait AppRootContext[C] {
  private val context: Ref[SyncIO, Option[C]] = Ref.unsafe[SyncIO, Option[C]](none[C])

  def init(c: C): SyncIO[Unit] =
    context
      .update(
        _.fold(c.some)(oldC =>
          throw new Exception(
            s"Attempt to reinitialize App Context to [$c], which was already initialized to [$oldC]."
          )
        )
      )

  def initIn[F[_]: LiftIO](c: C): F[Unit] = init(c).to[F]

  def get: SyncIO[C] =
    context.get.map(_.getOrElse(throw new Exception("Attempt to use uninitialized App Context")))

  def apply[F[_]: LiftIO]: F[C] = get.to[F]

  def flatMap[A, F[_]: LiftIO: FlatMap](f: C => F[A]): F[A] = apply[F].flatMap(f)

  def map[A, F[_]: LiftIO: Functor](f: C => A): F[A] = apply[F].map(f)

  def withCtx[A](f: C => A): A = get.map(f).unsafeRunSync()
}
