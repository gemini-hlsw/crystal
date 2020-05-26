package crystal

import cats.Applicative

package object implicits {
  implicit class OptionApplicativeUnitOps[F[_]: Applicative](opt: Option[F[Unit]]) {
    def orUnit: F[Unit] = opt.getOrElse(Applicative[F].unit)
  }
}
