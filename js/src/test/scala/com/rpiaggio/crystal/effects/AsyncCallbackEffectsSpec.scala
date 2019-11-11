package com.rpiaggio.crystal.effects

import cats.effect.Async
import cats.effect.laws.discipline.{AsyncTests, SyncTests}
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.kernel.Eq
import cats.tests.CatsSuite
import com.rpiaggio.crystal.effects.AsyncCallbackEffects._
import japgolly.scalajs.react.AsyncCallback

import scala.util.Left

final class AsyncCallbackEffectsSpec extends CatsSuite with TestInstances with AsyncCallbackArbitraries {
  implicit val ec: TestContext = TestContext()

  implicit def eqCallback[A](implicit A: Eq[A], ec: TestContext): Eq[AsyncCallback[A]] =
    new Eq[AsyncCallback[A]] {
      def eqv(x: AsyncCallback[A], y: AsyncCallback[A]): Boolean =
        eqFuture[A].eqv(x.unsafeToFuture(), y.unsafeToFuture())
    }

  checkAll("Async[AsyncCallback]", AsyncTests[AsyncCallback].async[Int, Int, Int])
}
