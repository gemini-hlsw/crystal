import cats.Monad
import cats.implicits._
import cats.Applicative

package object crystal {
  type StreamRenderer[A]          = ComponentTypes.StreamRenderer[A]
  type StreamRendererMod[F[_], A] = ComponentTypes.StreamRendererMod[F, A]
  type AppRoot[M]                 = ComponentTypes.AppRoot[M]

  implicit class UnitMonadOps[F[_]: Monad](f: F[Unit]) {
    def when(cond: F[Boolean]): F[Unit] =
      cond.flatMap(f.whenA)
  }
}

package crystal {
  trait ComponentTypes {
    type StreamRenderer[A]
    type StreamRendererMod[F[_], A]
    type AppRoot[M]
  }

  object ComponentTypes extends ComponentTypesForPlatform

  object implicits {
    implicit class OptionApplicativeUnitOps[F[_]: Applicative](opt: Option[F[Unit]]) {
      def orUnit: F[Unit] = opt.getOrElse(Applicative[F].unit)
    }
  }
}
