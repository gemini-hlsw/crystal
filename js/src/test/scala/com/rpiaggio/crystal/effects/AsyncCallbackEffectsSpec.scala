package com.rpiaggio.crystal.effects

import cats.effect.laws.discipline.SyncTests
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.kernel.Eq
import cats.tests.CatsSuite
import com.rpiaggio.crystal.effects.AsyncCallbackEffects._
import japgolly.scalajs.react.AsyncCallback

final class AsyncCallbackEffectsSpec extends CatsSuite with TestInstances with AsyncCallbackArbitraries {
  implicit val ec: TestContext = TestContext()

  implicit def eqCallback[A](implicit A: Eq[A], ec: TestContext): Eq[AsyncCallback[A]] =
    new Eq[AsyncCallback[A]] {
      def eqv(x: AsyncCallback[A], y: AsyncCallback[A]): Boolean =
        eqFuture[A].eqv(x.unsafeToFuture(), y.unsafeToFuture())
    }

    checkAll("Sync[AsyncCallback]", SyncTests[AsyncCallback].sync[Int, Int, Int])
}
