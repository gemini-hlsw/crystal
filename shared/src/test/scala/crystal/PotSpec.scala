// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.kernel.laws.discipline.EqTests
import cats.laws.discipline.SemigroupalTests.Isomorphisms
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.syntax.all._
import crystal.implicits._
import crystal.implicits.throwable._
import monocle.law.discipline.PrismTests
import munit.DisciplineSuite
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import arbitraries._

class PotSpec extends DisciplineSuite {
  implicit def iso: Isomorphisms[Pot] = Isomorphisms.invariant[Pot]

  checkAll(
    "Pot[Int].EqLaws",
    EqTests[Pot[Int]].eqv
  )

  checkAll(
    "Pot[Int].AlignLaws",
    AlignTests[Pot].align[Int, Int, Int, Int]
  )

  checkAll(
    "Pot[Int].MonadErrorLaws",
    MonadErrorTests[Pot, Throwable].monadError[Int, Int, String]
  )

  checkAll(
    "Pot[Int].TraverseLaws",
    TraverseTests[Pot].traverse[Int, Int, Int, Int, Option, Option]
  )

  checkAll(
    "Pot[Int].readyPrism",
    PrismTests(Pot.readyPrism[Int])
  )

  checkAll(
    "Pot[Int].errorPrism",
    PrismTests(Pot.errorPrism[Int])
  )

  property("Pending.toOption is None") {
    Pot.Pending.toOption.isEmpty
  }

  property("Pending.toOption is*") {
    Pot.Pending.isPending && !Pot.Pending.isReady && !Pot.Pending.isError
  }

  property("Pot[Int].toOption: Error(_) is None") {
    forAll((t: Throwable) => Pot.Error(t).toOption.isEmpty)
  }

  property("Pot[Int].toOption: Error(_) is*") {
    forAll((t: Throwable) => Pot.Error(t).isError && !Pot.Error(t).isReady && !Pot.Error(t).isReady)
  }

  property("Pot[Int].toOption: Ready(a) is Some(a)") {
    forAll((i: Int) => Pot.Ready(i).toOption === i.some)
  }

  property("Pot[Int].toOption: Ready(a) is*") {
    forAll((i: Int) => Pot.Ready(i).isReady && !Pot.Ready(i).isPending && !Pot.Ready(i).isError)
  }

  property("Pending.toOptionTry is None") {
    Pot.Pending.toOptionTry.isEmpty
  }

  property("Pot[Int].toOptionTry: Error(t) is Some(Failure(t))") {
    forAll((t: Throwable) => Pot.error[Int](t).toOptionTry.contains_(Failure(t)))
  }

  property("Pot[Int].toOptionTry: Ready(a) is Some(Success(a))") {
    forAll((i: Int) => Pot.Ready(i).toOptionTry === Success(i).some)
  }

  property("Pot[Int] (Any.ready): a.ready === Ready(a)") {
    forAll((i: Int) => i.ready === Pot.Ready(i))
  }

  property("Pot[Int] (Option.toPot): None.toPot === Pending") {
    Prop(
      none[Int].toPot match {
        case Pot.Pending => true
        case _           => false
      }
    )
  }

  property("Pot[Int] (Option.toPot): Some(a).toPot === Ready(a)") {
    forAll((i: Int) => i.some.toPot === Pot.Ready(i))
  }

  property("Pot[Int] (Option[Try].toPot): None.toPot === Pending") {
    Prop(
      none[Try[Int]].toPot match {
        case Pot.Pending => true
        case _           => false
      }
    )
  }

  property("Pot[Int] (Option[Try].toPot): Some(Failure[Int](t)).toPot === Error(t)") {
    forAll((t: Throwable) => Failure[Int](t).some.toPot === Pot.Error(t))
  }

  property("Pot[Int] (Option[Try].toPot): Some(Success(a).toPot) === Ready(a)") {
    forAll((i: Int) => Success(i).some.toPot === Pot.Ready(i))
  }

  property("Pot[Int] (Try.toPot): Failure[Int](t).toPot === Error(t)") {
    forAll((t: Throwable) => Failure[Int](t).toPot === Pot.Error(t))
  }

  property("Pot[Int] (Try.toPot): Success(a).toPot === Ready(a)") {
    forAll((i: Int) => Success(i).toPot === Pot.Ready(i))
  }

  property("Ready(Pending).flatten === Pending") {
    Pot.Ready(Pot.Pending).flatten === Pot.Pending
  }

  property("Pot[Pot[Int]] (Pot.flatten): Ready(Ready(a)).flatten === Ready(a)") {
    forAll((i: Int) => Pot.Ready(Pot.Ready(i)).flatten === Pot.Ready(i))
  }

  property("Pot[Pot[Int]] (Pot.flatten): Ready(Error(t)).flatten === Error(t)") {
    forAll { (t: Throwable) =>
      Pot[Pot[Int]](Pot.Error(t)).flatten === Pot.Error(t)
    }
  }

  // property("Ready(None).flatOpt === Pending") {
  //   Pot.Ready(None).flatOpt == Pot.Pending
  // }

  // property("Pot[Option[Int]] (Pot.flatten): Ready(Some(a)).flatOpt === Ready(a)") {
  //   forAll((i: Int) => Pot.Ready(Some(i)).flatOpt === Pot.Ready(i))
  // }
}
