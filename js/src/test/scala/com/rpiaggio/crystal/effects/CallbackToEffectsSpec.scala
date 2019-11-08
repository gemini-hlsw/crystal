package com.rpiaggio.crystal.effects

import cats.{Applicative, ApplicativeError, Defer, Eval, Monad, MonadError}
import cats.effect.{Bracket, ExitCase, IO, Sync, SyncIO}
import cats.effect.laws.discipline.{BracketTests, SyncTests}
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.kernel.Eq
import japgolly.scalajs.react.{Callback, CallbackTo, CatsReact}
import cats.tests.CatsSuite
import org.scalacheck.Arbitrary

final class CallbackToEffectsSpec extends CatsSuite with TestInstances {
  implicit val ec: TestContext = TestContext()


  import CallbackToEffects._


  implicit def arbitraryCallbackTo[A: Arbitrary]: Arbitrary[CallbackTo[A]] =
    Arbitrary(genApply[A])

  implicit def eqCallback[A](implicit A: Eq[A], ec: TestContext): Eq[CallbackTo[A]] =
    new Eq[CallbackTo[A]] {
      def eqv(x: CallbackTo[A], y: CallbackTo[A]): Boolean =
        eqFuture[A].eqv(x.asAsyncCallback.unsafeToFuture(), y.asAsyncCallback.unsafeToFuture())
    }

  checkAll("Sync[CallbackTo]", SyncTests[CallbackTo].sync[Int, Int, Int])
}
