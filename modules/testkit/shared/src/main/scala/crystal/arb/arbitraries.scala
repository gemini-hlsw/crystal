// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.arb

import cats.Id
import crystal.Pot
import crystal.PotOption
import crystal.ViewF
import org.scalacheck.*

import scala.annotation.targetName

import Arbitrary.*
import Gen.*

given [A: Arbitrary]: Arbitrary[Pot[A]] =
  Arbitrary(
    oneOf(
      Gen.const(Pot.Pending),
      arbitrary[Throwable].map(Pot.Error.apply),
      arbitrary[A].map(Pot.Ready.apply)
    )
  )

given [A](using Arbitrary[A => A]): Arbitrary[Pot[A] => Pot[A]] =
  Arbitrary(arbitrary[A => A].map(f => _.map(f)))

given [A: Arbitrary]: Arbitrary[PotOption[A]] =
  Arbitrary(
    oneOf(
      Gen.const(PotOption.Pending),
      arbitrary[Throwable].map(PotOption.Error.apply),
      Gen.const(PotOption.ReadyNone),
      arbitrary[A].map(PotOption.ReadySome.apply)
    )
  )

given viewFArb[A: Arbitrary]: Arbitrary[ViewF[Id, A]] = Arbitrary(
  arbitrary[A].map(a => ViewF.apply[Id, A](a, (f, cb) => cb(f(a))))
)

@targetName("potOptionFnArbitrary")
given [A](using fArb: Arbitrary[A => A]): Arbitrary[PotOption[A] => PotOption[A]] =
  Arbitrary(arbitrary[A => A].map(f => _.map(f)))
