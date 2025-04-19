// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Applicative
import cats.Eq
import cats.InvariantSemigroupal
import cats.Monad
import cats.syntax.all.*

export syntax.*

extension [F[_]: Monad](f: F[Unit])
  def when(cond: F[Boolean]): F[Unit] =
    cond.flatMap(f.whenA)

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

given [F[_]: Monad]: InvariantSemigroupal[ViewF[F, *]] with
  def product[A, B](fa: ViewF[F, A], fb: ViewF[F, B]): ViewF[F, (A, B)] =
    ViewF.apply(
      fa.get -> fb.get,
      (f, cb) =>
        fa.modCB( // Get current a value
          identity(_),
          oldA =>
            fb.modCB( // Get current b value
              identity(_),
              oldB =>
                fa.modCB( // Modify underlying a value
                  a => f(a, oldB)._1,
                  (prevA, newA) =>
                    fb.modCB( // Modify underlying b value
                      b => f(oldA, b)._2,
                      (prevB, newB) => cb((prevA, prevB), (newA, newB))
                    )
                )
            )
        )
    )

  def imap[A, B](fa: ViewF[F, A])(f: A => B)(g: B => A): ViewF[F, B] =
    fa.zoom(f)(mod => a => g(mod(f(a))))
