package crystal

trait ActionInterpreter[F[_], A[_[_]], T] {
  def of(view: View[F, T]): A[F]
}
