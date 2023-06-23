// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Applicative
import cats.Eq
import cats.FlatMap
import cats.Monad
import cats.effect.Ref
import cats.syntax.all.*

export syntax.*

extension [F[_]: Monad](f: F[Unit])
  def when(cond: F[Boolean]): F[Unit] =
    cond.flatMap(f.whenA)

def refModCB[F[_]: FlatMap, A](ref: Ref[F, A]): (A => A, A => F[Unit]) => F[Unit] =
  (f, cb) => ref.modify(f >>> (a => (a, a))) >>= cb

extension [F[_]: Applicative](opt: Option[F[Unit]])
  def orUnit: F[Unit] =
    opt.getOrElse(Applicative[F].unit)

object throwable {
  // Copied from cats-effect-laws utils.
  given Eq[Throwable] =
    new Eq[Throwable] {
      def eqv(x: Throwable, y: Throwable): Boolean =
        // All exceptions are non-terminating and given exceptions
        // aren't values (being mutable, they implement reference
        // equality), then we can't really test them reliably,
        // especially due to race conditions or outside logic
        // that wraps them (e.g. ExecutionException)
        (x ne null) == (y ne null)
    }
}
