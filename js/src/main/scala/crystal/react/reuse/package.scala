package crystal.react

import japgolly.scalajs.react.Reusability
import scala.reflect.ClassTag

package object reuse {
  type ==>[A, B] = Reuse[A => B]

  implicit class AnyReuseOps[A](private val a: A)                        extends AnyVal {
    def reuseAlways: Reuse[A] = Reuse.always(a)

    def reuseNever: Reuse[A] = Reuse.never(a)

    /*
     * Implements the idiom:
     *   `a.curryReusing( (A, B) => C )`
     * to create a `Reusable[B => C]` with `Reusability[A]`.
     *
     * Works for other arities too, as implemented in `Reuse.Curried1`.
     */
    def curryReusing: Reuse.Curried1[A] = Reuse.currying(a)
  }

  implicit class Tuple2ReuseOps[R, S](private val t: (R, S))             extends AnyVal {
    /*
     * Implements the idiom:
     *   `(a, b).curryReusing( (A, B, C) => D )`
     * to create a `Reusable[C => D]` with `Reusability[(A, B)]`.
     *
     * Works for other arities too, as implemented in `Reuse.Curried2`.
     */
    def curryReusing: Reuse.Curried2[R, S] = Reuse.currying(t._1, t._2)
  }

  implicit class Tuple3ReuseOps[R, S, T](private val t: (R, S, T))       extends AnyVal {
    /*
     * Implements the idiom:
     *   `(a, b, c).curryReusing( (A, B, C, D) => E )`
     * to create a `Reusable[D => E]` with `Reusability[(A, B, C)]`.
     *
     * Works for other arities too, as implemented in `Reuse.Curried3`.
     */
    def curryReusing: Reuse.Curried3[R, S, T] = Reuse.currying(t._1, t._2, t._3)
  }

  implicit class Fn1ReuseOps[R, B](private val fn: R => B)               extends AnyVal {
    /*
     * Implements the idiom:
     *   `(R => B).reuseCurrying(r)`
     * to create a `Reusable[B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[B] = Reuse.currying(r).in(fn)
  }

  implicit class Fn2ReuseOps[R, S, B](private val fn: (R, S) => B)       extends AnyVal {
    /*
     * Implements the idiom:
     *   `((R, S) => B).reuseCurrying(r)`
     * to create a `Reusable[S => B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[S => B] = Reuse.currying(r).in(fn)

    /*
     * Implements the idiom:
     *   `((R, S) => B).reuseCurrying(r, s)`
     * to create a `Reusable[B]` with `Reusability[(R, S)]`.
     */
    def reuseCurrying(
      r:         R,
      s:         S
    )(implicit
      classTagR: ClassTag[(R, S)],
      reuseR:    Reusability[(R, S)]
    ): Reuse[B] = Reuse.currying(r, s).in(fn)
  }

  implicit class Fn3ReuseOps[R, S, T, B](private val fn: (R, S, T) => B) extends AnyVal {
    /*
     * Implements the idiom:
     *   `((R, S, T) => B).reuseCurrying(r)`
     * to create a `Reusable[(S, T) => B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[(S, T) => B] = Reuse.currying(r).in(fn)

    /*
     * Implements the idiom:
     *   `((R, S, T) => B).reuseCurrying(r, s)`
     * to create a `Reusable[T => B]` with `Reusability[(R, S)]`.
     */
    def reuseCurrying(
      r:         R,
      s:         S
    )(implicit
      classTagR: ClassTag[(R, S)],
      reuseR:    Reusability[(R, S)]
    ): Reuse[T => B] = Reuse.currying(r, s).in(fn)

    /*
     * Implements the idiom:
     *   `((R, S, T) => B).reuseCurrying(r, s, t)`
     * to create a `Reusable[B]` with `Reusability[(R, S, T)]`.
     */
    def reuseCurrying(
      r:         R,
      s:         S,
      t:         T
    )(implicit
      classTagR: ClassTag[(R, S, T)],
      reuseR:    Reusability[(R, S, T)]
    ): Reuse[B] = Reuse.currying(r, s, t).in(fn)
  }

}
