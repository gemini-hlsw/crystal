// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

import cats.FlatMap
import cats.Monad
import cats.effect.Ref
import cats.syntax.all._

package object crystal {
  implicit class UnitMonadOps[F[_]: Monad](f: F[Unit]) {
    def when(cond: F[Boolean]): F[Unit] =
      cond.flatMap(f.whenA)
  }

  def refModCB[F[_]: FlatMap, A](ref: Ref[F, A]): (A => A, A => F[Unit]) => F[Unit] =
    (f, cb) => ref.modify(f >>> (a => (a, a))) >>= cb
}
