// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.reuse

import japgolly.scalajs.react.Reusability
import munit.ScalaCheckSuite
import org.scalacheck.Prop.*

import scala.language.implicitConversions

final class ReuseSpec extends ScalaCheckSuite {

  test("Reuse(function).by(value) syntax") {
    forAll { (u1: String, u2: String) =>
      val r1: Double ==> String = Reuse((q: Double) => s"$q $u1").by(u1)
      val r2: Double ==> String = Reuse((q: Double) => s"$q $u2").by(u2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[String]].test(u1, u2)
      )
    }
  }

  test("Reuse.by(value)(function) syntax") {
    forAll { (u1: String, u2: String) =>
      val r1: Double ==> String = Reuse.by(u1)((q: Double) => s"$q $u1")
      val r2: Double ==> String = Reuse.by(u2)((q: Double) => s"$q $u2")
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[String]].test(u1, u2)
      )
    }
  }

  def format(units: String, q: Double): String = s"$q $units"

  def format2(units: String, prefix: String, q: Double): String = s"$prefix $q $units"

  // Currying
  test("Reuse(function)(parameter) syntax") {
    forAll { (u1: String, u2: String) =>
      val r1: Double ==> String = Reuse(format)(u1)
      val r2: Double ==> String = Reuse(format)(u2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[String]].test(u1, u2)
      )
    }
  }

  test("Reuse(function2)(parameter) syntax") {
    forAll { (u1: String, u2: String) =>
      val r1: (String, Double) ==> String = Reuse(format2)(u1)
      val r2: (String, Double) ==> String = Reuse(format2)(u2)
      assertEquals(
        summon[Reusability[(String, Double) ==> String]].test(r1, r2),
        summon[Reusability[String]].test(u1, u2)
      )
    }
  }

  test("Reuse(function2)(parameter1, parameter2) syntax") {
    forAll { (u1: String, p1: String, u2: String, p2: String) =>
      val r1: Double ==> String = Reuse(format2)(u1, p1)
      val r2: Double ==> String = Reuse(format2)(u2, p2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[(String, String)]].test((u1, p1), (u2, p2))
      )
    }
  }

  test("Reuse(function2)(parameter1).curry(parameter2) syntax") {
    forAll { (u1: String, p1: String, u2: String, p2: String) =>
      val r1: Double ==> String = Reuse(format2)(u1).curry(p1)
      val r2: Double ==> String = Reuse(format2)(u2).curry(p2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[(String, String)]].test((u1, p1), (u2, p2))
      )
    }
  }

  test("Reuse.currying(paramter).in(function) syntax") {
    forAll { (u1: String, u2: String) =>
      val r1: Double ==> String = Reuse.currying(u1).in(format)
      val r2: Double ==> String = Reuse.currying(u2).in(format)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[String]].test(u1, u2)
      )
    }
  }

  test("Reuse.currying(parameter1, parameter2).in(function2) syntax") {
    forAll { (u1: String, p1: String, u2: String, p2: String) =>
      val r1: Double ==> String = Reuse.currying(u1, p1).in(format2)
      val r2: Double ==> String = Reuse.currying(u2, p2).in(format2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[(String, String)]].test((u1, p1), (u2, p2))
      )
    }
  }

  test("function.reuseCurrying(paramter) syntax") {
    forAll { (u1: String, u2: String) =>
      val r1: Double ==> String = format.reuseCurrying(u1)
      val r2: Double ==> String = format.reuseCurrying(u2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[String]].test(u1, u2)
      )
    }
  }

  test("function2.reuseCurrying(paramter1).curry(parameter2) syntax") {
    forAll { (u1: String, p1: String, u2: String, p2: String) =>
      val r1: Double ==> String = format2.reuseCurrying(u1).curry(p1)
      val r2: Double ==> String = format2.reuseCurrying(u2).curry(p2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[(String, String)]].test((u1, p1), (u2, p2))
      )
    }
  }

  test("function2.reuseCurrying(paramter1, parameter2) syntax") {
    forAll { (u1: String, p1: String, u2: String, p2: String) =>
      val r1: Double ==> String = format2.reuseCurrying(u1, p1)
      val r2: Double ==> String = format2.reuseCurrying(u2, p2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[(String, String)]].test((u1, p1), (u2, p2))
      )
    }
  }

  test("parameter.curryReusing.in(function) syntax") {
    forAll { (u1: String, u2: String) =>
      val r1: Double ==> String = u1.curryReusing.in(format)
      val r2: Double ==> String = u2.curryReusing.in(format)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[String]].test(u1, u2)
      )
    }
  }

  test("parameter1.curryReusing.in(function2).curry(parameter2) syntax") {
    forAll { (u1: String, p1: String, u2: String, p2: String) =>
      val r1: Double ==> String = u1.curryReusing.in(format2).curry(p1)
      val r2: Double ==> String = u2.curryReusing.in(format2).curry(p2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[(String, String)]].test((u1, p1), (u2, p2))
      )
    }
  }

  test("(parameter1, paramter2).curryReusing.in(function2) syntax") {
    forAll { (u1: String, p1: String, u2: String, p2: String) =>
      val r1: Double ==> String = (u1, p1).curryReusing.in(format2)
      val r2: Double ==> String = (u2, p2).curryReusing.in(format2)
      assertEquals(
        summon[Reusability[Double ==> String]].test(r1, r2),
        summon[Reusability[(String, String)]].test((u1, p1), (u2, p2))
      )
    }
  }

  given Reusability[Double] = Reusability.double(0.1)

  test("(parameter1, paramter2).curryReusing.in(function2).curry(parameter3) syntax") {
    forAll { (u1: String, p1: String, d1: Double, u2: String, p2: String, d2: Double) =>
      val r1: Reuse[String] = (u1, p1).curryReusing.in(format2).curry(d1)
      val r2: Reuse[String] = (u2, p2).curryReusing.in(format2).curry(d2)
      assertEquals(
        summon[Reusability[Reuse[String]]].test(r1, r2),
        summon[Reusability[(String, String, Double)]].test((u1, p1, d1), (u2, p2, d2))
      )
    }
  }
}
