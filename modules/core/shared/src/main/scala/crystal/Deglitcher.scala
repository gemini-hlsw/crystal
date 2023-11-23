// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.effect.Ref
import cats.effect.Temporal
import cats.syntax.all.*
import fs2.Pipe
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

final class Deglitcher[F[_]] private (
  waitUntil: Ref[F, FiniteDuration],
  timeout:   FiniteDuration
)(using F: Temporal[F]) {

  def throttle: F[Unit] =
    F.monotonic.flatMap(now => waitUntil.set(now + timeout))

  def debounce[A]: Pipe[F, A, A] =
    _.switchMap { a =>
      Stream.eval {
        def wait: F[Unit] =
          (waitUntil.get, F.monotonic).flatMapN { (waitUntil, now) =>
            if (waitUntil > now)
              F.sleep(waitUntil - now) *> wait
            else F.unit
          }

        wait.as(a)
      }
    }

}

object Deglitcher {
  def apply[F[_]](timeout: FiniteDuration)(using F: Temporal[F]): F[Deglitcher[F]] =
    F.monotonic.flatMap(F.ref(_)).map(new Deglitcher(_, timeout))
}
