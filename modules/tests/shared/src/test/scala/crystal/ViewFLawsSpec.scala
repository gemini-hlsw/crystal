// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Id
import cats.Invariant
import cats.laws.discipline.InvariantSemigroupalTests
import crystal.arb.given
import munit.DisciplineSuite

class ViewFLawsSpec extends DisciplineSuite with EqInstances {

  checkAll(
    "ViewF[Int].InvariantSemigroupalTests",
    InvariantSemigroupalTests[ViewF[Id, *]].invariantSemigroupal[Int, Int, Int]
  )

}
