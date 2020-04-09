package crystal

import scala.language.higherKinds
import monocle.Lens

// The difference between a View and a StateSnapshot is that the modifier doesn't act on the current value,
// but passes the modifier function to an external source of truth. Since we are defining no getter
// from such source of truth, a View is defined in terms of a modifier function instead of a setter.
case class View[F[_], A](value: A, mod: (A => A) => F[Unit]) {
  def set(a: A): F[Unit] = mod(_ => a)

  def zoom[B](get: A => B)(modB: (B => B) => A => A): View[F, B] =
    View(get(value), (f: B => B) => mod(modB(f)))

  def zoomL[B](lens: Lens[A, B]): View[F, B] =
    zoom(lens.get)(lens.modify)
}
