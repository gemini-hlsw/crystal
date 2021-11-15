package crystal.react.reuse

import japgolly.scalajs.react.Reusability

import scala.reflect.ClassTag

/*
 * Supports construction via the pattern `Reuse.currying(valueWithReusability : R).in( (R[, ...]) => B )`
 * and similar for higher arities.
 */
trait CurryingSyntax {
  class Curried1[R](val r: R) {
    /*
     * Add another curried value.
     */
    def and[S](s: S): Curried2[R, S] = new Curried2(r, s)

    /*
     * Given R and a R => B, build a Reuse[B].
     */
    def in[B](
      fn:        R => B
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[B] =
      Reuse.by(r)(fn(r))

    /*
     * Given R and a (R, S) => B, build a S ==> B.
     */
    def in[S, B](
      fn:        (R, S) => B
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[S => B] =
      Reuse.by(r)(s => fn(r, s))

    /*
     * Given R and a (R, S, T) => B, build a (S, T) ==> B.
     */
    def in[S, T, B](
      fn:        (R, S, T) => B
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[(S, T) => B] =
      Reuse.by(r)((s, t) => fn(r, s, t))
  }

  class Curried2[R, S](val r: R, val s: S) {
    /*
     * Add another curried value.
     */
    def and[T](t: T): Curried3[R, S, T] = new Curried3(r, s, t)

    /*
     * Given R, S and a (R, S) => B, build a Reuse[B].
     */
    def in[B](
      fn:        (R, S) => B
    )(implicit
      classTagR: ClassTag[(R, S)],
      reuseR:    Reusability[(R, S)]
    ): Reuse[B] =
      Reuse.by((r, s))(fn(r, s))

    /*
     * Given R, S and a (R, S, T) => B, build a T ==> B.
     */
    def in[T, B](
      fn:        (R, S, T) => B
    )(implicit
      classTagR: ClassTag[(R, S)],
      reuseR:    Reusability[(R, S)]
    ): Reuse[T => B] =
      Reuse.by((r, s))(t => fn(r, s, t))
  }

  class Curried3[R, S, T](val r: R, val s: S, val t: T) {
    /*
     * Given R, S, T and a (R, S, T) => B, build a Reuse[B].
     */
    def in[B](
      fn:        (R, S, T) => B
    )(implicit
      classTagR: ClassTag[(R, S, T)],
      reuseR:    Reusability[(R, S, T)]
    ): Reuse[B] =
      Reuse.by((r, s, t))(fn(r, s, t))

    /*
     * Given R, S, T and a (R, S, T, U) => B, build a U ==> B.
     */
    def in[U, B](
      fn:        (R, S, T, U) => B
    )(implicit
      classTagR: ClassTag[(R, S, T)],
      reuseR:    Reusability[(R, S, T)]
    ): Reuse[U => B] =
      Reuse.by((r, s, t))(u => fn(r, s, t, u))
  }
}
