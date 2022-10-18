// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import org.scalacheck._

import Arbitrary._
import Gen._

object arbitraries {
  implicit def arbPot[A: Arbitrary]: Arbitrary[Pot[A]] =
    Arbitrary(
      oneOf(
        Gen.const(Pot.Pending),
        arbitrary[Throwable].map(Pot.Error.apply),
        arbitrary[A].map(Pot.Ready.apply)
      )
    )

  implicit def arbPotF[A](implicit fArb: Arbitrary[A => A]): Arbitrary[Pot[A] => Pot[A]] =
    Arbitrary(arbitrary[A => A].map(f => _.map(f)))

  implicit def arbPotOption[A: Arbitrary]: Arbitrary[PotOption[A]] =
    Arbitrary(
      oneOf(
        Gen.const(PotOption.Pending),
        arbitrary[Throwable].map(PotOption.Error.apply),
        Gen.const(PotOption.ReadyNone),
        arbitrary[A].map(PotOption.ReadySome.apply)
      )
    )

  implicit def arbPotOptionF[A](implicit
    fArb: Arbitrary[A => A]
  ): Arbitrary[PotOption[A] => PotOption[A]] =
    Arbitrary(arbitrary[A => A].map(f => _.map(f)))
}
