import cats.FlatMap
import cats.Monad
import cats.effect.Ref
import cats.syntax.all._

package object crystal {
  type StreamRenderer[A]    = ComponentTypes.StreamRenderer[A]
  type StreamRendererMod[A] = ComponentTypes.StreamRendererMod[A]

  implicit class UnitMonadOps[F[_]: Monad](f: F[Unit]) {
    def when(cond: F[Boolean]): F[Unit] =
      cond.flatMap(f.whenA)
  }

  def refModCB[F[_]: FlatMap, A](ref: Ref[F, A]): ((A => A), A => F[Unit]) => F[Unit] =
    (f, cb) => ref.modify(f >>> (a => (a, a))) >>= cb
}

package crystal {
  trait ComponentTypes {
    type StreamRenderer[A]
    type StreamRendererMod[A]
  }

  object ComponentTypes extends ComponentTypesForPlatform
}
