package crystal

import cats.implicits._
import cats.effect.concurrent.Deferred
import cats.effect.Async

abstract class AppCtx[F[_]: Async, C] {
  private val context: Deferred[F, C] = Deferred.unsafeUncancelable[F, C]

  def init(c: C): F[Unit] = context.complete(c)

  def ctx: F[C] = context.get

  def flatMap[A](f: C => F[A]): F[A] = ctx.flatMap(f)

  def map[A](f: C => A): F[A] = ctx.map(f)
}
