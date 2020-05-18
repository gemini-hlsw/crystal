package crystal.data

import munit.DisciplineSuite
import cats.laws.discipline.MonadTests
import arbitraries._
import cats.implicits._
import cats.kernel.laws.discipline.EqTests
import org.scalacheck.Prop.forAll
import crystal.data.implicits._

class PotSpec extends DisciplineSuite {
  checkAll(
    "Pot.EqLaws",
    EqTests[Pot[Int]].eqv
  )

  checkAll(
    "Pot.MonadLaws",
    MonadTests[Pot].monad[Int, Int, String]
  )

  property("Pot.toOption: Pending(_) === None") {
    forAll((l: Long) => Pot.Pending[Int](l).toOption === none)

  }

  property("Pot.toOption: Error(_) === None") {
    forAll((t: Throwable) => Pot.Error[Int](t).toOption === none)
  }

  property("Pot.toOption: Ready(a) === Some(a)") {
    forAll((i: Int) => Pot.Ready(i).toOption === i.some)
  }
}
