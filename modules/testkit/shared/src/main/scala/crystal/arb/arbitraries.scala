// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.arb

import cats.Id
import crystal.Pot
import crystal.PotOption
import crystal.ViewF
import org.scalacheck.*

import Arbitrary.*
import Gen.*

given [A: Arbitrary]: Arbitrary[Pot[A]] = Arbitrary:
  oneOf(
    Gen.const(Pot.Pending),
    arbitrary[Throwable].map(Pot.Error.apply),
    arbitrary[A].map(Pot.Ready.apply)
  )

given [A: Cogen]: Cogen[Pot[A]] =
  Cogen[Option[Option[A]]].contramap(_.toOptionTry.map(_.toOption))

given [A: Arbitrary]: Arbitrary[PotOption[A]] = Arbitrary:
  oneOf(
    Gen.const(PotOption.Pending),
    arbitrary[Throwable].map(PotOption.Error.apply),
    Gen.const(PotOption.ReadyNone),
    arbitrary[A].map(PotOption.ReadySome.apply)
  )

given [A: Cogen]: Cogen[PotOption[A]] =
  Cogen[Option[Option[Option[A]]]].contramap(_.toOptionTry.map(_.toOption))

given viewFArb[A: Arbitrary]: Arbitrary[ViewF[Id, A]] = Arbitrary:
  arbitrary[A].map(a => ViewF.apply[Id, A](a, (f, cb) => cb(a, f(a))))

given [A: Cogen]: Cogen[ViewF[Id, A]] =
  Cogen[A].contramap(_.get)
