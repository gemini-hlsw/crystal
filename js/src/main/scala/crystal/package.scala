import cats.Monad
import cats.implicits._

import scala.language.higherKinds

package object crystal {
  implicit class UnitMonadOps[F[_]: Monad](f: F[Unit]) {
    def when(cond: F[Boolean]): F[Unit] =
      cond.flatMap(f.whenA)
  }
}
