package crystal

import munit.DisciplineSuite
import arbitraries._
import cats.syntax.all._
import cats.kernel.laws.discipline.EqTests
import org.scalacheck.Prop.forAll
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.SemigroupalTests.Isomorphisms
import crystal.implicits._
import crystal.implicits.throwable._
import scala.util.Failure
import scala.util.Success
import org.scalacheck.Prop
import scala.util.Try

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
    forAll((l: Long) => Pending(l).toOption.isEmpty)
  }

  property("Pot[Int].toOption: Pending(_) isPending") {
    forAll((l: Long) => Pending(l).isPending && !Pending(l).isReady && !Pending(l).isError)
  }

  property("Pot[Int].toOption: Error(_) is None") {
    forAll((t: Throwable) => Error(t).toOption.isEmpty)
  }

  property("Pot[Int].toOption: Error(_) isError") {
    forAll((t: Throwable) => Error(t).isError && !Error(t).isReady && !Error(t).isReady)
  }

  property("Pot[Int].toOption: Ready(a) is Some(a)") {
    forAll((i: Int) => Ready(i).toOption === i.some)
  }

  property("Pot[Int].toOption: Ready(a) isReady") {
    forAll((i: Int) => Ready(i).isReady && !Ready(i).isPending && !Ready(i).isError)
  }

  property("Pot[Int].toTryOption: Pending(_) is None") {
    forAll((l: Long) => Pending(l).toTryOption.isEmpty)
  }

  property("Pot[Int].toTryOption: Error(t) is Some(Failure(t))") {
    forAll((t: Throwable) => Pot.error[Int](t).toTryOption.contains_(Failure(t)))
  }

  property("Pot[Int].toTryOption: Ready(a) is Some(Success(a))") {
    forAll((i: Int) => Ready(i).toTryOption === Success(i).some)
  }

  property("Pot[Int] (Any.ready): a.ready === Ready(a)") {
    forAll((i: Int) => i.ready === Ready(i))
  }

  property("Pot[Int] (Option.toPot): None.toPot === Pending(_)") {
    Prop(
      none[Int].toPot match {
        case Pending(_) => true
        case _          => false
      }
    )
  }

  property("Pot[Int] (Option.toPot): Some(a).toPot === Ready(a)") {
    forAll((i: Int) => i.some.toPot === Ready(i))
  }

  property("Pot[Int] (Option[Try].toPot): None.toPot === Pending(_)") {
    Prop(
      none[Try[Int]].toPot match {
        case Pending(_) => true
        case _          => false
      }
    )
  }

  property("Pot[Int] (Option[Try].toPot): Some(Failure[Int](t)).toPot === Error(t)") {
    forAll((t: Throwable) => Failure[Int](t).some.toPot === Error(t))
  }

  property("Pot[Int] (Option[Try].toPot): Some(Success(a).toPot) === Ready(a)") {
    forAll((i: Int) => Success(i).some.toPot === Ready(i))
  }

  property("Pot[Int] (Try.toPot): Failure[Int](t).toPot === Error(t)") {
    forAll((t: Throwable) => Failure[Int](t).toPot === Error(t))
  }

  property("Pot[Int] (Try.toPot): Success(a).toPot === Ready(a)") {
    forAll((i: Int) => Success(i).toPot === Ready(i))
  }

  property("Pot[Pot[Int]] (Pot.flatten): Ready(Ready(a)).flatten === Ready(a)") {
    forAll((i: Int) => Ready(Ready(i)).flatten === Ready(i))
  }

  property("Pot[Pot[Int]] (Pot.flatten): Ready(Error(t)).flatten === Error(t)") {
    forAll { (t: Throwable) =>
      Pot[Pot[Int]](Error(t)).flatten === Error(t)
    }
  }
}
