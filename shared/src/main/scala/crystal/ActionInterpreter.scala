package crystal

trait ActionInterpreterOpt[F[_], A[_[_]], T] {
  def of(view: ViewOpt[F, T]): A[F]
}

trait ActionInterpreter[F[_], A[_[_]], T] {
  def of(view: View[F, T]): A[F]
}
