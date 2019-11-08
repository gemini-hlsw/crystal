package com.rpiaggio.crystal.effects

import cats.effect.laws.discipline.SyncTests
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.kernel.Eq
import japgolly.scalajs.react.CallbackTo
import cats.tests.CatsSuite
import org.scalacheck.Arbitrary
import CallbackToEffects._

final class CallbackToEffectsSpec extends CatsSuite with TestInstances {
  implicit val ec: TestContext = TestContext()

  implicit def arbitraryCallbackTo[A: Arbitrary]: Arbitrary[CallbackTo[A]] =
    Arbitrary(genApply[A])

  implicit def eqCallback[A](implicit A: Eq[A], ec: TestContext): Eq[CallbackTo[A]] =
    new Eq[CallbackTo[A]] {
      def eqv(x: CallbackTo[A], y: CallbackTo[A]): Boolean =
        eqFuture[A].eqv(x.asAsyncCallback.unsafeToFuture(), y.asAsyncCallback.unsafeToFuture())
    }

  checkAll("Sync[CallbackTo]", SyncTests[CallbackTo].sync[Int, Int, Int])
}
