// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Eq
import cats.Id
import org.scalacheck.Arbitrary
import org.scalacheck.Cogen

// https://github.com/circe/circe/blob/cddf7f28c37aaff46407e097b48135d8b411520b/modules/testing/shared/src/main/scala/io/circe/testing/EqInstances.scala
trait EqInstances {

  /**
   * The number of arbitrary values that will be considered when checking for equality.
   */
  protected def functionEqualityCheckCount: Int = 16

  private[this] def arbitraryValues[A](using A: Arbitrary[A]): LazyList[A] =
    LazyList.continually(A.arbitrary.sample).flatten

  given [A: Eq: Arbitrary: Cogen]: Eq[ViewF[Id, A]] = Eq.instance { (v1, v2) =>
    Eq[A].eqv(v1.get, v2.get) && arbitraryValues[(A => A)].take(functionEqualityCheckCount).forall {
      f =>
        var newA1: A | Null = null
        var newA2: A | Null = null
        v1.modCB(f, a => newA1 = a)
        v2.modCB(f, a => newA2 = a)

        (newA1 != null && newA2 != null) && Eq[A].eqv(newA1.nn, newA2.nn)
    }
  }
}
