// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.reuse

import japgolly.scalajs.react.Reusability

import scala.annotation.targetName
import scala.reflect.ClassTag

protected trait AppliedSyntax {

  /*
   * Supports construction via the pattern `Reuse(reusedValue).by(valueWithReusability)`
   */
  class Applied[A](valueA: => A) {
    val value: () => A = () => valueA

    def by[R: ClassTag: Reusability](reuseByR: R): Reuse[A] =
      Reuse.by(reuseByR)(valueA)

    def always: Reuse[A] = Reuse.by(())(valueA)

    def self(using ClassTag[A], Reusability[A]): Reuse[A] =
      Reuse.by(valueA)(valueA)
  }

  extension [A, R, S, B](aa: Applied[((R, S) => B)])
    /*
     * Given a (R, S) => B, instantiate R and build a S ==> B.
     */
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[S => B] =
      Reuse.by(r)(s => aa.value()(r, s))

    /*
     * Given a (R, S) => B, instantiate R and S and build a Reuse[B].
     */
    def apply(r: R, s: S)(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[B] =
      Reuse.by((r, s))(aa.value()(r, s))

  extension [A, R, S, T, B](aa: Applied[((R, S, T) => B)])
    /*
     * Given a (R, S, T) => B , instantiate R and build a (S, T) ==> B.
     */
    @targetName("reuseFn3Apply1")
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[(S, T) => B] =
      Reuse.by(r)((s, t) => aa.value()(r, s, t))

    /*
     * Given a (R, S, T) => B , instantiate R and S and build a T ==> B.
     */
    @targetName("reuseFn3Apply2")
    def apply(r: R, s: S)(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[T => B] =
      Reuse.by((r, s))(t => aa.value()(r, s, t))

    /*
     * Given a (R, S, T) => B , instantiate R, S and T and build a Reuse[B].
     */
    @targetName("reuseFn3Apply3")
    def apply(r: R, s: S, t: T)(using ClassTag[(R, S, T)], Reusability[(R, S, T)]): Reuse[B] =
      Reuse.by((r, s, t))(aa.value()(r, s, t))

  extension [A, R, S, T, U, B](aa: Applied[((R, S, T, U) => B)])
    /*
     * Given a (R, S, T, U) => B , instantiate R and build a (S, T, U) ==> B.
     */
    @targetName("reuseFn4Apply1")
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[(S, T, U) => B] =
      Reuse.by(r)((s, t, u) => aa.value()(r, s, t, u))

    /*
     * Given a (R, S, T, U) => B , instantiate R and S and build a (T, U) ==> B.
     */
    @targetName("reuseFn4Apply2")
    def apply(r: R, s: S)(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[(T, U) => B] =
      Reuse.by((r, s))((t, u) => aa.value()(r, s, t, u))

    /*
     * Given a (R, S, T, U) => B , instantiate R, S and T and build a U ==> B.
     */
    @targetName("reuseFn4Apply3")
    def apply(r: R, s: S, t: T)(using
      ClassTag[(R, S, T)],
      Reusability[(R, S, T)]
    ): Reuse[U => B] =
      Reuse.by((r, s, t))(u => aa.value()(r, s, t, u))

    /*
     * Given a (R, S, T, U) => B , instantiate R, S, T and U and build a Reuse[B].
     */
    @targetName("reuseFn4Apply3")
    def apply(r: R, s: S, t: T, u: U)(using
      ClassTag[(R, S, T, U)],
      Reusability[(R, S, T, U)]
    ): Reuse[B] =
      Reuse.by((r, s, t, u))(aa.value()(r, s, t, u))

  extension [A, R, S, T, U, V, B](aa: Applied[((R, S, T, U, V) => B)])
    /*
     * Given a (R, S, T, U, V) => B , instantiate R and build a (S, T, U, V) ==> B.
     */
    @targetName("reuseFn5Apply1")
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[(S, T, U, V) => B] =
      Reuse.by(r)((s, t, u, v) => aa.value()(r, s, t, u, v))

    /*
     * Given a (R, S, T, U, V) => B , instantiate R and S and build a (T, U, V) ==> B.
     */
    @targetName("reuseFn5Apply2")
    def apply(r: R, s: S)(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[(T, U, V) => B] =
      Reuse.by((r, s))((t, u, v) => aa.value()(r, s, t, u, v))

    /*
     * Given a (R, S, T, U, V) => B , instantiate R, S and T and build a (U, V) ==> B.
     */
    @targetName("reuseFn5Apply3")
    def apply(r: R, s: S, t: T)(using
      ClassTag[(R, S, T)],
      Reusability[(R, S, T)]
    ): Reuse[(U, V) => B] =
      Reuse.by((r, s, t))((u, v) => aa.value()(r, s, t, u, v))

    /*
     * Given a (R, S, T, U, V) => B , instantiate R, S, T and U and build a V ==> B Reuse[B].
     */
    @targetName("reuseFn5Apply4")
    def apply(r: R, s: S, t: T, u: U)(using
      ClassTag[(R, S, T, U)],
      Reusability[(R, S, T, U)]
    ): Reuse[V => B] =
      Reuse.by((r, s, t, u))(v => aa.value()(r, s, t, u, v))

    /*
     * Given a (R, S, T, U, V) => B , instantiate R, S, T, U and V and build a Reuse[B].
     */
    @targetName("reuseFn5Apply5")
    def apply(r: R, s: S, t: T, u: U, v: V)(using
      ClassTag[(R, S, T, U, V)],
      Reusability[(R, S, T, U, V)]
    ): Reuse[B] =
      Reuse.by((r, s, t, u, v))(aa.value()(r, s, t, u, v))

  extension [A, R, S, T, U, V, W, B](aa: Applied[((R, S, T, U, V, W) => B)])
    /*
     * Given a (R, S, T, U, V, W) => B , instantiate R and build a (S, T, U, V, W) ==> B.
     */
    @targetName("reuseFn6Apply1")
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[(S, T, U, V, W) => B] =
      Reuse.by(r)((s, t, u, v, w) => aa.value()(r, s, t, u, v, w))

    /*
     * Given a (R, S, T, U, V, W) => B , instantiate R and S and build a (T, U, V, W) ==> B.
     */
    @targetName("reuseFn6Apply2")
    def apply(r: R, s: S)(using ClassTag[(R, S)], Reusability[(R, S)]): Reuse[(T, U, V, W) => B] =
      Reuse.by((r, s))((t, u, v, w) => aa.value()(r, s, t, u, v, w))

    /*
     * Given a (R, S, T, U, V, W) => B , instantiate R, S and T and build a (U, V, W) ==> B.
     */
    @targetName("reuseFn6Apply3")
    def apply(r: R, s: S, t: T)(using
      ClassTag[(R, S, T)],
      Reusability[(R, S, T)]
    ): Reuse[(U, V, W) => B] =
      Reuse.by((r, s, t))((u, v, w) => aa.value()(r, s, t, u, v, w))

    /*
     * Given a (R, S, T, U, V, W) => B , instantiate R, S, T and U and build a (V, W) ==> B.
     */
    @targetName("reuseFn6Apply4")
    def apply(r: R, s: S, t: T, u: U)(using
      ClassTag[(R, S, T, U)],
      Reusability[(R, S, T, U)]
    ): Reuse[(V, W) => B] =
      Reuse.by((r, s, t, u))((v, w) => aa.value()(r, s, t, u, v, w))

    /*
     * Given a (R, S, T, U, V, W) => B , instantiate R, S, T, U and V and build a W ==> B.
     */
    @targetName("reuseFn6Apply5")
    def apply(r: R, s: S, t: T, u: U, v: V)(using
      ClassTag[(R, S, T, U, V)],
      Reusability[(R, S, T, U, V)]
    ): Reuse[W => B] =
      Reuse.by((r, s, t, u, v))(w => aa.value()(r, s, t, u, v, w))

    /*
     * Given a (R, S, T, U, V, W) => B , instantiate R, S, T, U, V and W and build a Reuse[B].
     */
    @targetName("reuseFn6Apply6")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W)(using
      ClassTag[(R, S, T, U, V, W)],
      Reusability[(R, S, T, U, V, W)]
    ): Reuse[B] =
      Reuse.by((r, s, t, u, v, w))(aa.value()(r, s, t, u, v, w))

  extension [A, R, S, T, U, V, W, X, B](aa: Applied[((R, S, T, U, V, W, X) => B)])
    /*
     * Given a (R, S, T, U, V, W, X) => B , instantiate R and build a (S, T, U, V, W, X) ==> B.
     */
    @targetName("reuseFn7Apply1")
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[(S, T, U, V, W, X) => B] =
      Reuse.by(r)((s, t, u, v, w, x) => aa.value()(r, s, t, u, v, w, x))

    /*
     * Given a (R, S, T, U, V, W, X) => B , instantiate R and S and build a (T, U, V, W, X) ==> B.
     */
    @targetName("reuseFn7Apply2")
    def apply(r: R, s: S)(using
      ClassTag[(R, S)],
      Reusability[(R, S)]
    ): Reuse[(T, U, V, W, X) => B] =
      Reuse.by((r, s))((t, u, v, w, x) => aa.value()(r, s, t, u, v, w, x))

    /*
     * Given a (R, S, T, U, V, W, X) => B , instantiate R, S and T and build a (U, V, W, X) ==> B.
     */
    @targetName("reuseFn7Apply3")
    def apply(r: R, s: S, t: T)(using
      ClassTag[(R, S, T)],
      Reusability[(R, S, T)]
    ): Reuse[(U, V, W, X) => B] =
      Reuse.by((r, s, t))((u, v, w, x) => aa.value()(r, s, t, u, v, w, x))

    /*
     * Given a (R, S, T, U, V, W, X) => B , instantiate R, S, T and U and build a (V, W, X) ==> B.
     */
    @targetName("reuseFn7Apply4")
    def apply(r: R, s: S, t: T, u: U)(using
      ClassTag[(R, S, T, U)],
      Reusability[(R, S, T, U)]
    ): Reuse[(V, W, X) => B] =
      Reuse.by((r, s, t, u))((v, w, x) => aa.value()(r, s, t, u, v, w, x))

    /*
     * Given a (R, S, T, U, V, W, X) => B , instantiate R, S, T, U and V and build a (W, X) ==> B.
     */
    @targetName("reuseFn7Apply5")
    def apply(r: R, s: S, t: T, u: U, v: V)(using
      ClassTag[(R, S, T, U, V)],
      Reusability[(R, S, T, U, V)]
    ): Reuse[(W, X) => B] =
      Reuse.by((r, s, t, u, v))((w, x) => aa.value()(r, s, t, u, v, w, x))

    /*
     * Given a (R, S, T, U, V, W, W, X) => B , instantiate R, S, T, U, V and W and build a X ==> B.
     */
    @targetName("reuseFn7Apply6")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W)(using
      ClassTag[(R, S, T, U, V, W)],
      Reusability[(R, S, T, U, V, W)]
    ): Reuse[X => B] =
      Reuse.by((r, s, t, u, v, w))(x => aa.value()(r, s, t, u, v, w, x))

    /*
     * Given a (R, S, T, U, V, W, W, X) => B , instantiate R, S, T, U, V, W and X and build a Reuse[B].
     */
    @targetName("reuseFn7Apply7")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W, x: X)(using
      ClassTag[(R, S, T, U, V, W, X)],
      Reusability[(R, S, T, U, V, W, X)]
    ): Reuse[B] =
      Reuse.by((r, s, t, u, v, w, x))(aa.value()(r, s, t, u, v, w, x))

  extension [A, R, S, T, U, V, W, X, Y, B](aa:    Applied[((R, S, T, U, V, W, X, Y) => B)])
    /*
     * Given a (R, S, T, U, V, W, X, Y) => B , instantiate R and build a (S, T, U, V, W, X, Y) ==> B.
     */
    @targetName("reuseFn8Apply1")
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[(S, T, U, V, W, X, Y) => B] =
      Reuse.by(r)((s, t, u, v, w, x, y) => aa.value()(r, s, t, u, v, w, x, y))

    /*
     * Given a (R, S, T, U, V, W, X, Y) => B , instantiate R and S and build a (T, U, V, W, X, Y) ==> B.
     */
    @targetName("reuseFn8Apply2")
    def apply(r: R, s: S)(using
      ClassTag[(R, S)],
      Reusability[(R, S)]
    ): Reuse[(T, U, V, W, X, Y) => B] =
      Reuse.by((r, s))((t, u, v, w, x, y) => aa.value()(r, s, t, u, v, w, x, y))

    /*
     * Given a (R, S, T, U, V, W, X, Y) => B , instantiate R, S and T and build a (U, V, W, X, Y) ==> B.
     */
    @targetName("reuseFn8Apply3")
    def apply(r: R, s: S, t: T)(using
      ClassTag[(R, S, T)],
      Reusability[(R, S, T)]
    ): Reuse[(U, V, W, X, Y) => B] =
      Reuse.by((r, s, t))((u, v, w, x, y) => aa.value()(r, s, t, u, v, w, x, y))

    /*
     * Given a (R, S, T, U, V, W, X, Y) => B , instantiate R, S, T and U and build a (V, W, X, Y) ==> B.
     */
    @targetName("reuseFn8Apply4")
    def apply(r: R, s: S, t: T, u: U)(using
      ClassTag[(R, S, T, U)],
      Reusability[(R, S, T, U)]
    ): Reuse[(V, W, X, Y) => B] =
      Reuse.by((r, s, t, u))((v, w, x, y) => aa.value()(r, s, t, u, v, w, x, y))

    /*
     * Given a (R, S, T, U, V, W, X, Y) => B , instantiate R, S, T, U and V and build a (W, X, Y) ==> B.
     */
    @targetName("reuseFn8Apply5")
    def apply(r: R, s: S, t: T, u: U, v: V)(using
      ClassTag[(R, S, T, U, V)],
      Reusability[(R, S, T, U, V)]
    ): Reuse[(W, X, Y) => B] =
      Reuse.by((r, s, t, u, v))((w, x, y) => aa.value()(r, s, t, u, v, w, x, y))

    /*
     * Given a (R, S, T, U, V, W, W, X, Y) => B , instantiate R, S, T, U, V and W and build a (X, Y) ==> B.
     */
    @targetName("reuseFn8Apply6")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W)(using
      ClassTag[(R, S, T, U, V, W)],
      Reusability[(R, S, T, U, V, W)]
    ): Reuse[(X, Y) => B] =
      Reuse.by((r, s, t, u, v, w))((x, y) => aa.value()(r, s, t, u, v, w, x, y))

    /*
     * Given a (R, S, T, U, V, W, W, X, Y) => B , instantiate R, S, T, U, V, W and X and build a Y => Reuse[B].
     */
    @targetName("reuseFn8Apply7")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W, x: X)(using
      ClassTag[(R, S, T, U, V, W, X)],
      Reusability[(R, S, T, U, V, W, X)]
    ): Reuse[Y => B] =
      Reuse.by((r, s, t, u, v, w, x))(y => aa.value()(r, s, t, u, v, w, x, y))

    /*
     * Given a (R, S, T, U, V, W, W, X, Y) => B , instantiate R, S, T, U, V, W and X and build a Reuse[B].
     */
    @targetName("reuseFn8Apply8")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W, x: X, y: Y)(using
      ClassTag[(R, S, T, U, V, W, X, Y)],
      Reusability[(R, S, T, U, V, W, X, Y)]
    ): Reuse[B] =
      Reuse.by((r, s, t, u, v, w, x, y))(aa.value()(r, s, t, u, v, w, x, y))
  extension [A, R, S, T, U, V, W, X, Y, Z, B](aa: Applied[((R, S, T, U, V, W, X, Y, Z) => B)])
    /*
     * Given a (R, S, T, U, V, W, X, Y, Z) => B , instantiate R and build a (S, T, U, V, W, X, Y, Z) ==> B.
     */
    @targetName("reuseFn9Apply1")
    def apply(r: R)(using ClassTag[R], Reusability[R]): Reuse[(S, T, U, V, W, X, Y, Z) => B] =
      Reuse.by(r)((s, t, u, v, w, x, y, z) => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, X, Y, Z) => B , instantiate R and S and build a (T, U, V, W, X, Y, Z) ==> B.
     */
    @targetName("reuseFn9Apply2")
    def apply(r: R, s: S)(using
      ClassTag[(R, S)],
      Reusability[(R, S)]
    ): Reuse[(T, U, V, W, X, Y, Z) => B] =
      Reuse.by((r, s))((t, u, v, w, x, y, z) => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, X, Y, Z) => B , instantiate R, S and T and build a (U, V, W, X, Y, Z) ==> B.
     */
    @targetName("reuseFn9Apply3")
    def apply(r: R, s: S, t: T)(using
      ClassTag[(R, S, T)],
      Reusability[(R, S, T)]
    ): Reuse[(U, V, W, X, Y, Z) => B] =
      Reuse.by((r, s, t))((u, v, w, x, y, z) => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, X, Y, Z) => B , instantiate R, S, T and U and build a (V, W, X, Y, Z) ==> B.
     */
    @targetName("reuseFn9Apply4")
    def apply(r: R, s: S, t: T, u: U)(using
      ClassTag[(R, S, T, U)],
      Reusability[(R, S, T, U)]
    ): Reuse[(V, W, X, Y, Z) => B] =
      Reuse.by((r, s, t, u))((v, w, x, y, z) => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, X, Y, Z) => B , instantiate R, S, T, U and V and build a (W, X, Y, Z) ==> B.
     */
    @targetName("reuseFn9Apply5")
    def apply(r: R, s: S, t: T, u: U, v: V)(using
      ClassTag[(R, S, T, U, V)],
      Reusability[(R, S, T, U, V)]
    ): Reuse[(W, X, Y, Z) => B] =
      Reuse.by((r, s, t, u, v))((w, x, y, z) => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, W, X, Y, Z) => B , instantiate R, S, T, U, V and W and build a (X, Y, Z) ==> B.
     */
    @targetName("reuseFn9Apply6")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W)(using
      ClassTag[(R, S, T, U, V, W)],
      Reusability[(R, S, T, U, V, W)]
    ): Reuse[(X, Y, Z) => B] =
      Reuse.by((r, s, t, u, v, w))((x, y, z) => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, W, X, Y, Z) => B , instantiate R, S, T, U, V, W and X and build a (Y, Z) => Reuse[B].
     */
    @targetName("reuseFn9Apply7")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W, x: X)(using
      ClassTag[(R, S, T, U, V, W, X)],
      Reusability[(R, S, T, U, V, W, X)]
    ): Reuse[(Y, Z) => B] =
      Reuse.by((r, s, t, u, v, w, x))((y, z) => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, W, X, Y, Z) => B , instantiate R, S, T, U, V, W, X and Y and build a Z => Reuse[B].
     */
    @targetName("reuseFn9Apply8")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W, x: X, y: Y)(using
      ClassTag[(R, S, T, U, V, W, X, Y)],
      Reusability[(R, S, T, U, V, W, X, Y)]
    ): Reuse[Z => B] =
      Reuse.by((r, s, t, u, v, w, x, y))(z => aa.value()(r, s, t, u, v, w, x, y, z))

    /*
     * Given a (R, S, T, U, V, W, W, X, Y, Z) => B , instantiate R, S, T, U, V, W, X, Y, Z and build a Reuse[B].
     */
    @targetName("reuseFn9Apply9")
    def apply(r: R, s: S, t: T, u: U, v: V, w: W, x: X, y: Y, z: Z)(using
      ClassTag[(R, S, T, U, V, W, X, Y, Z)],
      Reusability[(R, S, T, U, V, W, X, Y, Z)]
    ): Reuse[B] =
      Reuse.by((r, s, t, u, v, w, x, y, z))(aa.value()(r, s, t, u, v, w, x, y, z))
}
