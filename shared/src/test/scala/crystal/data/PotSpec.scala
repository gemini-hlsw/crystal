package crystal.data

import munit.DisciplineSuite
import arbitraries._
import cats.implicits._
import cats.kernel.laws.discipline.EqTests
import org.scalacheck.Prop.forAll
import crystal.data.implicits._
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.SemigroupalTests.Isomorphisms
import Pot._

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

  property("Pot[Int].toOption: Pending(_) is None") {
    forAll((l: Long) => Pot.Pending[Int](l).toOption === none)

  }

  property("Pot[Int].toOption: Error(_) is None") {
    forAll((t: Throwable) => Pot.Error[Int](t).toOption === none)
  }

  property("Pot[Int].toOption: Ready(a) is Some(a)") {
    forAll((i: Int) => Pot.Ready(i).toOption === i.some)
  }

  property("Pot[Int].asReady: a.asReady === Ready(a)") {
    forAll((i: Int) => i.asReady === Ready(i))
  }
}
