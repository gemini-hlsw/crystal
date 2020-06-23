import cats.effect.IO
import cats.effect.ContextShift
import cats.implicits._
import cats.kernel.Eq
import monocle.macros.Lenses
import scala.concurrent.ExecutionContext
import monocle.Optional
import monocle.Traversal
import monocle.function.Possible.possible

package object crystal {
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
}

package crystal {
  @Lenses
  case class Wrap[A](a: A)
  object Wrap {
    implicit def eqWrap[A: Eq]: Eq[Wrap[A]] = Eq.fromUniversalEquals
  }

  @Lenses
  case class WrapOpt[A](a: Option[A])
  object WrapOpt {
    def aOpt[A]: Optional[WrapOpt[A], A] =
      WrapOpt.a.composeOptional(possible[Option[A], A])

    implicit def eqWrapOpt[A: Eq]: Eq[WrapOpt[A]] = Eq.fromUniversalEquals
  }

  @Lenses
  case class WrapList[A](a: List[A])
  object WrapList {
    def aList[A]: Traversal[WrapList[A], A]         =
      WrapList.a.composeTraversal(Traversal.fromTraverse[List, A])
    implicit def eqWrapList[A: Eq]: Eq[WrapList[A]] = Eq.fromUniversalEquals
  }
}
