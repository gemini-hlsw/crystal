// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.reuse

import japgolly.scalajs.react.Reusability

import scala.reflect.ClassTag

/*
 * Wraps a (lazy) `value` of type `A` and an associated `reuseBy` of a hidden type `B`,
 * delegating `Reusability` to `Reusability[B]`.
 *
 * In other words, delegates existential `Reusability` of instances of `A` to an existing
 * universal `Reusability` of `B`, while associating instances of `A` to instances of `B`.
 *
 * It's particularly useful to provide `Reusability` for functions or VDOM elements.
 *
 * When used for functions, it differs from `scalajs-react`'s `Reusable.fn` mainly in that
 * the reference equality of the wrapped function is not checked: `Reusability` is
 * computed solely based on the provided `reuseBy` value. This allows using inline idioms
 * like:
 *
 *   `Component(Reuse.currying(props).in( (props, text: String) => ...: VdomNode)`
 *
 * in which `Component` would expect a `Reuse[String => VdomNode]` as a parameter. In this
 * case, the wrapped `String => VdomNode` function is reused as long as `props` can be reused.
 *
 * A number of convenience constructors, methods and implicits are provided in order to
 * support a wide variety of use cases. Notably:
 *  * `(A, B, ...) ==> Z` is a type alias for `Reusable[(A, B, ...) => Z]`.
 *  * `A` is automatically unwrapped when expecting `A` and a `Reuse[A]` is provided.
 *  * A `Reuse[A]` is automatically converted to `Reusable[A]` when needed.
 *
 */
trait Reuse[+A] {
  type B

  lazy val value: A = getValue()

  protected[reuse] val getValue: () => A

  protected[reuse] val reuseBy: B // We need to store it to combine into tuples when currying.

  protected[reuse] implicit val classTag: ClassTag[B]

  protected[reuse] implicit val reusability: Reusability[B]

  def addReuseBy[R: Reusability](r: R): Reuse[A]        = Reuse.by((reuseBy, r))(value)
  def addReuseByFrom[C](r:          Reuse[C]): Reuse[A] = addReuseBy(r.reuseBy)(r.reusability)

  def replaceReuseBy[R: Reusability: ClassTag](r: R): Reuse[A] = Reuse.by(r)(value)
  def replaceReuseByFrom[C](r: Reuse[C]): Reuse[A] =
    replaceReuseBy(r.reuseBy)(r.reusability, r.classTag)

  def map[C](f: A => C): Reuse[C] = Reuse.by(reuseBy)(f(value))

  def zip[C](that: Reuse[C]): Reuse[(A, C)] = {
    implicit val thatReuse: Reusability[that.B] = that.reusability
    Reuse.by((reuseBy, that.reuseBy))((value, that.value))
  }

  def zipMap[C, D](that: Reuse[C])(f: (A, C) => D): Reuse[D] =
    zip(that).map(f.tupled)
}

object Reuse extends AppliedSyntax with CurryingSyntax with CurrySyntax with ReusableInterop {
  implicit def reusability[A]: Reusability[Reuse[A]] =
    Reusability.apply { (reuseA, reuseB) =>
      reuseA.classTag == reuseB.classTag &&
      reuseA.reusability.test(reuseA.reuseBy, reuseB.reuseBy.asInstanceOf[reuseA.B]) &&
      reuseB.reusability.test(reuseA.reuseBy.asInstanceOf[reuseB.B], reuseB.reuseBy)
    }

  /*
   * Constructs a `Reuse[A]` by using the pattern `Reuse.by(valueWithReusability)(reusedValue)`.
   */
  def by[A, R](reuseByR: R) = new AppliedBy(reuseByR)

  def always[A](a: A): Reuse[A] = by(())(a)(implicitly[ClassTag[Unit]], Reusability.always)

  def never[A](a: A): Reuse[A] = by(())(a)(implicitly[ClassTag[Unit]], Reusability.never)

  /*
   * Constructs a `Reuse[A]` by using the pattern `Reuse(reusedValue).by(valueWithReusability)`.
   */
  def apply[A](value: => A): Applied[A] = new Applied(value)

  /*
   * Constructs a reusable function by using the pattern
   * `Reuse.currying(valueWithReusability : R).in( (R[, ...]) => B )`.
   */
  def currying[R](r: R): Curried1[R] = new Curried1(r)

  /*
   * Constructs a reusable function by using the pattern
   * `Reuse.currying(value1WithReusability : R, value2: S).in( (R, S[, ...]) => B )`.
   */
  def currying[R, S](r: R, s: S): Curried2[R, S] = new Curried2(r, s)

  /*
   * Constructs a reusable function by using the pattern
   * `Reuse.currying(value1WithReusability : R, value2: S, value3: T).in( (R, S, T[, ...]) => B )`.
   */
  def currying[R, S, T](r: R, s: S, t: T): Curried3[R, S, T] = new Curried3(r, s, t)

  /*
   * Constructs a reusable function by using the pattern
   * `Reuse.currying(value1WithReusability : R, value2: S, value3: T, value4: U).in( (R, S, T, U[, ...]) => B )`.
   */
  def currying[R, S, T, U](r: R, s: S, t: T, u: U): Curried4[R, S, T, U] = new Curried4(r, s, t, u)

  /*
   * Constructs a reusable function by using the pattern
   * `Reuse.currying(value1WithReusability : R, value2: S, value3: T, value4: U, value5: V).in( (R, S, T, U, V[, ...]) => B )`.
   */
  def currying[R, S, T, U, V](r: R, s: S, t: T, u: U, v: V): Curried5[R, S, T, U, V] =
    new Curried5(r, s, t, u, v)

  /*
   * Supports construction via the pattern `Reuse.by(valueWithReusability)(reusedValue)`
   */
  class AppliedBy[R](reuseByR: R) {
    /*
     * Auto-tuple 2-parameter function in order to be able to use (S, T) ==> B notation
     * when constructing via the pattern `Reuse.by(value)(function)`.
     */
    def apply[A, S, T, B](
      fn:                 (S, T) => B
    )(implicit classTagR: ClassTag[R], reuseR: Reusability[R]): (S, T) ==> B =
      apply(fn.tupled)

    /*
     * Auto-tuple 3-parameter function in order to be able to use (S, T, U) ==> B notation
     * when constructing via the pattern `Reuse.by(value)(function)`.
     */
    def apply[A, S, T, U, B](
      fn:                 (S, T, U) => B
    )(implicit classTagR: ClassTag[R], reuseR: Reusability[R]): (S, T, U) ==> B =
      apply(fn.tupled)

    /*
     * Auto-tuple 4-parameter function in order to be able to use (S, T, U, V) ==> B notation
     * when constructing via the pattern `Reuse.by(value)(function)`.
     */
    def apply[A, S, T, U, V, B](
      fn:                 (S, T, U, V) => B
    )(implicit classTagR: ClassTag[R], reuseR: Reusability[R]): (S, T, U, V) ==> B =
      apply(fn.tupled)

    /*
     * Auto-tuple 5-parameter function in order to be able to use (S, T, U, V, W) ==> B notation
     * when constructing via the pattern `Reuse.by(value)(function)`.
     */
    def apply[A, S, T, U, V, W, B](
      fn:                 (S, T, U, V, W) => B
    )(implicit classTagR: ClassTag[R], reuseR: Reusability[R]): (S, T, U, V, W) ==> B =
      apply(fn.tupled)

    def apply[A](valueA: => A)(implicit classTagR: ClassTag[R], reuseR: Reusability[R]): Reuse[A] =
      new Reuse[A] {
        type B = R

        protected[reuse] val getValue = () => valueA

        protected[reuse] val reuseBy = reuseByR

        protected[reuse] val classTag = classTagR

        protected[reuse] val reusability = reuseR
      }
  }

}
