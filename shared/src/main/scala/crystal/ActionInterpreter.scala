package crystal

import crystal.data.ViewF
import crystal.data.ViewOptF

trait ActionInterpreterOpt[F[_], A[_[_]], T] {
  def of(view: ViewOptF[F, T]): A[F]
}

trait ActionInterpreter[F[_], A[_[_]], T] {
  def of(view: ViewF[F, T]): A[F]
}
