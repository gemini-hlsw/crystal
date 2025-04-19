// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

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
    def in[B](fn: R => B)(using ClassTag[R], Reusability[R]): Reuse[B] =
      Reuse.by(r)(fn(r))

    /*
     * Given R and a (R, S) => B, build a S ==> B.
     */
    def in[S, B](fn: (R, S) => B)(using ClassTag[R], Reusability[R]): Reuse[S => B] =
      Reuse.by(r)(s => fn(r, s))

    /*
     * Given R and a (R, S, T) => B, build a (S, T) ==> B.
     */
    def in[S, T, B](fn: (R, S, T) => B)(using ClassTag[R], Reusability[R]): Reuse[(S, T) => B] =
      Reuse.by(r)((s, t) => fn(r, s, t))

    /*
     * Given R and a (R, S, T, U) => B, build a (S, T, U) ==> B.
     */
    def in[S, T, U, B](
      fn: (R, S, T, U) => B
    )(using ClassTag[R], Reusability[R]): Reuse[(S, T, U) => B] =
      Reuse.by(r)((s, t, u) => fn(r, s, t, u))

    /*
     * Given R and a (R, S, T, U, V) => B, build a (S, T, U, V) ==> B.
     */
    def in[S, T, U, V, B](
      fn: (R, S, T, U, V) => B
    )(using ClassTag[R], Reusability[R]): Reuse[(S, T, U, V) => B] =
      Reuse.by(r)((s, t, u, v) => fn(r, s, t, u, v))
  }

  class Curried2[R, S](val r: R, val s: S) {
    /*
     * Add another curried value.
     */
    def and[T](t: T): Curried3[R, S, T] = new Curried3(r, s, t)

    /*
     * Given R, S and a (R, S) => B, build a Reuse[B].
     */
    def in[B](fn: (R, S) => B)(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[B] =
      Reuse.by((r, s))(fn(r, s))

    /*
     * Given R, S and a (R, S, T) => B, build a T ==> B.
     */
    def in[T, B](fn: (R, S, T) => B)(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[T => B] =
      Reuse.by((r, s))(t => fn(r, s, t))

    /*
     * Given R, S and a (R, S, T, U) => B, build a (T, U) ==> B.
     */
    def in[T, U, B](
      fn: (R, S, T, U) => B
    )(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[(T, U) => B] =
      Reuse.by((r, s))((t, u) => fn(r, s, t, u))

    /*
     * Given R, S and a (R, S, T, U, V) => B, build a (T, U, V) ==> B.
     */
    def in[T, U, V, B](
      fn: (R, S, T, U, V) => B
    )(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[(T, U, V) => B] =
      Reuse.by((r, s))((t, u, v) => fn(r, s, t, u, v))
  }

  class Curried3[R, S, T](val r: R, val s: S, val t: T) {
    /*
     * Add another curried value.
     */
    def and[U](u: U): Curried4[R, S, T, U] = new Curried4(r, s, t, u)

    /*
     * Given R, S, T and a (R, S, T) => B, build a Reuse[B].
     */
    def in[B](fn: (R, S, T) => B)(using ClassTag[(R, S, T)], Reusability[(R, S, T)]): Reuse[B] =
      Reuse.by((r, s, t))(fn(r, s, t))

    /*
     * Given R, S, T and a (R, S, T, U) => B, build a U ==> B.
     */
    def in[U, B](
      fn: (R, S, T, U) => B
    )(using ClassTag[(R, S, T)], Reusability[(R, S, T)]): Reuse[U => B] =
      Reuse.by((r, s, t))(u => fn(r, s, t, u))

    /*
     * Given R, S, T and a (R, S, T, U, V) => B, build a (U, V) ==> B.
     */
    def in[U, V, B](
      fn: (R, S, T, U, V) => B
    )(using ClassTag[(R, S, T)], Reusability[(R, S, T)]): Reuse[(U, V) => B] =
      Reuse.by((r, s, t))((u, v) => fn(r, s, t, u, v))
  }

  class Curried4[R, S, T, U](val r: R, val s: S, val t: T, val u: U) {
    /*
     * Add another curried value.
     */
    def and[V](v: V): Curried5[R, S, T, U, V] = new Curried5(r, s, t, u, v)

    /*
     * Given R, S, T, U and a (R, S, T, U) => B, build a Reuse[B].
     */
    def in[B](
      fn: (R, S, T, U) => B
    )(using ClassTag[(R, S, T, U)], Reusability[(R, S, T, U)]): Reuse[B] =
      Reuse.by((r, s, t, u))(fn(r, s, t, u))

    /*
     * Given R, S, T, U and a (R, S, T, U, V) => B, build a V ==> B.
     */
    def in[V, B](
      fn: (R, S, T, U, V) => B
    )(using ClassTag[(R, S, T, U)], Reusability[(R, S, T, U)]): Reuse[V => B] =
      Reuse.by((r, s, t, u))(v => fn(r, s, t, u, v))
  }

  class Curried5[R, S, T, U, V](val r: R, val s: S, val t: T, val u: U, val v: V) {
    /*
     * Given R, S, T, U, V and a (R, S, T, U, V) => B, build a Reuse[B].
     */
    def in[B](
      fn: (R, S, T, U, V) => B
    )(using ClassTag[(R, S, T, U, V)], Reusability[(R, S, T, U, V)]): Reuse[B] =
      Reuse.by((r, s, t, u, v))(fn(r, s, t, u, v))
  }

}
