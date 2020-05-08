package crystal

import munit.DisciplineSuite
import cats.laws.discipline.FunctorTests
import arbitraries._
import cats.implicits._

class CtxSpec extends DisciplineSuite {
  checkAll(
    "Ctx.FunctorLaws",
    FunctorTests[Ctx[Int, *]].functor[Int, Int, String]
  )
}
