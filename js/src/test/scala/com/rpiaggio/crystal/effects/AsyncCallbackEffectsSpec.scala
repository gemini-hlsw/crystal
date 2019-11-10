package com.rpiaggio.crystal.effects

import cats.MonadError
import cats.effect.IO
import cats.effect.laws.discipline.SyncTests
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.kernel.Eq
import cats.laws.discipline.ApplicativeErrorTests
import cats.tests.CatsSuite
import com.rpiaggio.crystal.effects.AsyncCallbackEffects._
import japgolly.scalajs.react.{AsyncCallback, Callback, CallbackTo, CatsReact}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck._

import scala.util.{Either, Failure, Try}

final class AsyncCallbackEffectsSpec extends CatsSuite with TestInstances {
  implicit val ec: TestContext = TestContext()

  val genCallback: Gen[Callback] = Callback.empty

  implicit val arbitraryCallback: Arbitrary[Callback] = Arbitrary(genCallback)


  implicit val cogenCallback: Cogen[Callback] = Cogen(_ => 0L)

  def genAsync[A: Arbitrary]: Gen[AsyncCallback[A]] =
    arbitrary[(Try[A] => Callback) => Callback].map(AsyncCallback.apply)


  implicit def arbitraryAsyncCallback[A: Arbitrary]: Arbitrary[AsyncCallback[A]] =
    Arbitrary(genAsync[A])

  implicit def eqCallback[A](implicit A: Eq[A], ec: TestContext): Eq[AsyncCallback[A]] =
    new Eq[AsyncCallback[A]] {
      def eqv(x: AsyncCallback[A], y: AsyncCallback[A]): Boolean =
        eqFuture[A].eqv(x.unsafeToFuture(), y.unsafeToFuture())
    }

//  checkAll("ApplicativeError[AsyncCallback, Throwable]", ApplicativeErrorTests[AsyncCallback, Throwable](reactAsyncCallbackCatsInstance2).applicativeError[Int, Int, Int])
    checkAll("Sync[AsyncCallback]", SyncTests[AsyncCallback].sync[Int, Int, Int])
}
