// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Eq
import cats.Id
import cats.Invariant
import cats.laws.discipline.InvariantTests
import cats.laws.discipline.SemigroupalTests
import munit.DisciplineSuite

import arbitraries.given

class ViewFLawsSpec extends DisciplineSuite {

  private given viewEq[A: Eq]: Eq[ViewF[Id, A]] = Eq.by(_.get)

  checkAll(
    "ViewF[Int].InvariantLaws",
    InvariantTests[ViewF[Id, *]].invariant[Int, Int, String]
  )

  checkAll(
    "ViewF[Int].SemigroupalLaws",
    SemigroupalTests[ViewF[Id, *]].semigroupal[Int, Int, String]
  )
}
