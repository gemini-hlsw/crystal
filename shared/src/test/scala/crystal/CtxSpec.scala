package crystal

import munit.ScalaCheckSuite
import org.typelevel.discipline.munit.Discipline
import cats.laws.discipline.FunctorTests
import arbitraries._
import cats.implicits._

class CtxSpec extends ScalaCheckSuite with Discipline {
  checkAll(
    "Ctx.FunctorLaws",
    FunctorTests[Ctx[Int, *]].functor[Int, Int, String]
  )
}
