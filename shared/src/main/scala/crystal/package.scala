import cats.Monad
import cats.implicits._

package object crystal {
  type StreamRenderer[A] = ComponentTypes.StreamRenderer[A]
  type StreamRendererMod[F[_], A] = ComponentTypes.StreamRendererMod[F, A]
  type AppRoot[M] = ComponentTypes.AppRoot[M]

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
    implicit class AnyToCtxOps[A](val a: A) extends AnyVal {
      def inCtx[C](implicit ctx: C): Ctx[C, A] =
        Ctx(a, ctx)
    }
  }
}
