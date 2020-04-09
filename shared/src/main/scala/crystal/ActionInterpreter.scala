package crystal

import scala.language.higherKinds

trait ActionInterpreter[F[_], A[_[_]], T] {
  def of(view: View[F, T]): A[F]
}
