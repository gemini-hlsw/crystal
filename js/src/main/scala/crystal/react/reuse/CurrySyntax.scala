// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.reuse

import japgolly.scalajs.react.Reusability

import scala.annotation.targetName
import scala.reflect.ClassTag

protected trait CurrySyntax:
  /*
   * Support instantiating the parameter of a R ==> B via the `.curry(...)` method.
   */
  extension [A, R, B](ra: Reuse[R => B])
    def curry(
      r:         R
    )(using
      classTagR: ClassTag[(ra.B, R)],
      reuseR:    Reusability[R]
    ): Reuse[B] = {
      given Reusability[ra.B] = ra.reusability
      Reuse.by((ra.reuseBy, r))(ra.value(r))
    }

  /*
   * Support instantiating some or all of the parameters of a (R, S) ==> B via
   * the `.curry(...)` method.
   */
  // extension [A, R, S, B](ra: Reuse[A])(using ev: A =:= ((R, S) => B))
  extension [A, R, S, B](ra: Reuse[(R, S) => B])
    @targetName("reuse1CurryFn2")
    def curry(
      r:         R
    )(using
      classTagR: ClassTag[(ra.B, R)],
      reuseR:    Reusability[R]
    ): Reuse[S => B] = {
      given Reusability[ra.B] = ra.reusability
      Reuse.by((ra.reuseBy, r))(s => ra.value(r, s))
    }

  // Auto tupling/untupling to support (R, S) ==> B syntax. Since ==> is a type alias,
  // (R, S) is interpreted as a tuple in that case.
  given [A, R, S, B]: Conversion[Reuse[((R, S) => B)], (R, S) ==> B] =
    _.map(f => f.tupled)

  given [A, R, S, B]: Conversion[Reuse[(((R, S)) => B)], Reuse[(R, S) => B]] =
    _.map(f => (r, s) => f((r, s)))

  /*
   * Support instantiating some or all of the parameters of a (R, S, T) ==> B via
   * the `.curry(...)` method.
   */
  extension [A, R, S, T, B](ra: Reuse[(R, S, T) => B])
    @targetName("reuse1CurryFn3")
    def curry(
      r:         R
    )(using
      classTagR: ClassTag[(ra.B, R)],
      reuseR:    Reusability[R]
    ): Reuse[(S, T) => B] = {
      given Reusability[ra.B] = ra.reusability
      Reuse.by((ra.reuseBy, r))((s, t) => ra.value(r, s, t))
    }

  // Auto tupling/untupling to support (R, S, T) ==> B syntax. Since ==> is a type alias,
  // (R, S, T) is interpreted as a tuple in that case.
  @targetName("tupledReuseFn3")
  given [A, R, S, T, B]: Conversion[Reuse[((R, S, T) => B)], (R, S, T) ==> B] =
    _.map(f => f.tupled)

  @targetName("untupledReuseFn3")
  given [A, R, S, T, B]: Conversion[Reuse[(((R, S, T)) => B)], Reuse[(R, S, T) => B]] =
    _.map(f => (r, s, t) => f((r, s, t)))
