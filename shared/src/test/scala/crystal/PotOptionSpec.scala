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

class PotOptionSpec extends DisciplineSuite {
  implicit def iso: Isomorphisms[PotOption] = Isomorphisms.invariant[PotOption]

  checkAll(
    "PotOptionSpec[Int].EqLaws",
    EqTests[PotOption[Int]].eqv
  )

  checkAll(
    "PotOption[Int].AlignLaws",
    AlignTests[PotOption].align[Int, Int, Int, Int]
  )

  checkAll(
    "PotOption[Int].MonadErrorLaws",
    MonadErrorTests[PotOption, Throwable].monadError[Int, Int, String]
  )

  checkAll(
    "PotOption[Int].TraverseLaws",
    TraverseTests[PotOption].traverse[Int, Int, Int, Int, Option, Option]
  )

  checkAll(
    "PotOption[Int].readySomePrism",
    PrismTests(PotOption.readySomePrism[Int])
  )

  checkAll(
    "PotOption[Int].errorPrism",
    PrismTests(PotOption.errorPrism[Int])
  )

  property("Pending.toOption is None") {
    PotOption.Pending.toOption.isEmpty
  }

  property("Pending.toOption is*") {
    PotOption.Pending.isPending && !PotOption.Pending.isReady && !PotOption.Pending.isError && !PotOption.Pending.isDefined
  }

  property("PotOption[Int].toOption: Error(_) is None") {
    forAll((t: Throwable) => PotOption.Error(t).toOption.isEmpty)
  }

  property("PotOption[Int].toOption: Error(_) is*") {
    forAll((t: Throwable) =>
      PotOption.Error(t).isError &&
        !PotOption.Error(t).isReady &&
        !PotOption.Error(t).isReady &&
        !PotOption.Error(t).isDefined
    )
  }

  property("ReadyNone.toOption is None") {
    PotOption.ReadyNone.toOption.isEmpty
  }

  property("ReadyNone.toOption is*") {
    !PotOption.ReadyNone.isPending && PotOption.ReadyNone.isReady && !PotOption.ReadyNone.isError & !PotOption.ReadyNone.isDefined
  }

  property("PotOption[Int].toOption: ReadySome(a) is Some(a)") {
    forAll((i: Int) => PotOption.ReadySome(i).toOption === i.some)
  }

  property("PotOption[Int].toOption: Ready(a) is*") {
    forAll((i: Int) =>
      PotOption.ReadySome(i).isReady &&
        !PotOption.ReadySome(i).isPending &&
        !PotOption.ReadySome(i).isError &&
        PotOption.ReadySome(i).isDefined
    )
  }

  property("Pending.toOptionTry is None") {
    PotOption.Pending.toOptionTry.isEmpty
  }

  property("PotOption[Int].toOptionTry: Error(t) is Some(Failure(t))") {
    forAll((t: Throwable) => PotOption.error[Int](t).toOptionTry.contains_(Failure(t)))
  }

  property("PotOption[Int].toOptionTry: ReadySome(a) is Some(Success(a))") {
    forAll((i: Int) => PotOption.ReadySome(i).toOptionTry === Success(i).some)
  }

  property("PotOption[Int] (Any.ready): a.ready === Ready(a)") {
    forAll((i: Int) => i.ready === Pot.Ready(i))
  }

  property("PotOption[Int] (Option.toPotOption): None.toPotOption === ReadyNone") {
    Prop(
      none[Int].toPotOption match {
        case PotOption.ReadyNone => true
        case _                   => false
      }
    )
  }

  property("PotOption[Int] (Option.toPotOption): Some(a).toPotOption === ReadySome(a)") {
    forAll((i: Int) => i.some.toPotOption === PotOption.ReadySome(i))
  }

  property("PotOption[Int] (Option[Try].toPotOption): None.toPotOption === Pending") {
    Prop(
      none[Try[Int]].toPotOption match {
        case PotOption.Pending => true
        case _                 => false
      }
    )
  }

  property(
    "PotOption[Int] (Option[Try].toPotOption): Some(Failure[Int](t)).toPotOption === Error(t)"
  ) {
    forAll((t: Throwable) => Failure[Int](t).some.toPotOption === PotOption.Error(t))
  }

  property(
    "PotOption[Int] (Option[Try].toPotOption): Some(Success(a).toPotOption) === ReadySome(a)"
  ) {
    forAll((i: Int) => Success(i).some.toPotOption === PotOption.ReadySome(i))
  }

  property("PotOption[Int] (Try.toPotOption): Failure[Int](t).toPotOption === Error(t)") {
    forAll((t: Throwable) => Failure[Int](t).toPotOption === PotOption.Error(t))
  }

  property("PotOption[Int] (Try.toPotOption): Success(a).toPotOption === ReadySome(a)") {
    forAll((i: Int) => Success(i).toPotOption === PotOption.ReadySome(i))
  }

  property("ReadySome(Pending).flatten === Pending") {
    PotOption.ReadySome(PotOption.Pending).flatten === PotOption.Pending
  }

  property(
    "PotOption[PotOption[Int]] (PotOption.flatten): ReadySome(ReadySome(a)).flatten === ReadySome(a)"
  ) {
    forAll((i: Int) =>
      PotOption.ReadySome(PotOption.ReadySome(i)).flatten === PotOption.ReadySome(i)
    )
  }

  property(
    "PotOption[PotOption[Int]] (PotOption.flatten): ReadySome(ReadyNone).flatten === ReadyNone"
  ) {
    PotOption.ReadySome(PotOption.ReadyNone).flatten === PotOption.ReadyNone
  }

  property(
    "PotOption[PotOption[Int]] (PotOption.flatten): ReadySome(Error(t)).flatten === Error(t)"
  ) {
    forAll { (t: Throwable) =>
      PotOption[PotOption[Int]](PotOption.Error(t)).flatten === PotOption.Error(t)
    }
  }
}
