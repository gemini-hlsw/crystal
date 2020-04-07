import cats.Monad
import cats.implicits._

import scala.language.higherKinds

package object crystal {
  type StreamRenderer[A] = ComponentTypes.StreamRenderer[A]
  type StreamRendererMod[F[_], A] = ComponentTypes.StreamRendererMod[F, A]
  type AppRoot[F[_], C, M] = ComponentTypes.AppRoot[F, C, M]

  implicit class UnitMonadOps[F[_]: Monad](f: F[Unit]) {
    def when(cond: F[Boolean]): F[Unit] =
      cond.flatMap(f.whenA)
  }
}

package crystal {
  trait ComponentTypes {
    type StreamRenderer[A]
    type StreamRendererMod[F[_], A]
    type AppRoot[F[_], C, M]
  }

  object ComponentTypes extends ComponentTypesForPlatform
}