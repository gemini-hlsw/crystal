package com.rpiaggio.crystal.effects

import cats.effect.laws.discipline.SyncTests
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.kernel.Eq
import cats.tests.CatsSuite
import com.rpiaggio.crystal.effects.AsyncCallbackEffects._
import japgolly.scalajs.react.AsyncCallback
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

final class AsyncCallbackEffectsSpec extends CatsSuite with TestInstances {
  implicit val ec: TestContext = TestContext()

  def genApply[A: Arbitrary]: Gen[AsyncCallback[A]] =
    arbitrary[A].map(AsyncCallback.point(_))

  implicit def arbitraryCallbackTo[A: Arbitrary]: Arbitrary[AsyncCallback[A]] =
    Arbitrary(genApply[A])

  implicit def eqCallback[A](implicit A: Eq[A], ec: TestContext): Eq[AsyncCallback[A]] =
    new Eq[AsyncCallback[A]] {
      def eqv(x: AsyncCallback[A], y: AsyncCallback[A]): Boolean =
        eqFuture[A].eqv(x.unsafeToFuture(), y.unsafeToFuture())
    }

  checkAll("Sync[AsyncCallback]", SyncTests[AsyncCallback].sync[Int, Int, Int])
}
