import cats.Monad
import cats.syntax.all._

package object crystal {
  type StreamRenderer[A]    = ComponentTypes.StreamRenderer[A]
  type StreamRendererMod[A] = ComponentTypes.StreamRendererMod[A]

  implicit class UnitMonadOps[F[_]: Monad](f: F[Unit]) {
    def when(cond: F[Boolean]): F[Unit] =
      cond.flatMap(f.whenA)
  }
}

package crystal {
  trait ComponentTypes {
    type StreamRenderer[A]
    type StreamRendererMod[A]
  }

  object ComponentTypes extends ComponentTypesForPlatform
}
