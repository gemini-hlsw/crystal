package com.rpiaggio.crystal.effects

import cats.{Applicative, ApplicativeError, Defer, Eval, Monad, MonadError}
import cats.effect.{Bracket, ExitCase, IO, Sync, SyncIO}
import cats.effect.laws.discipline.{BracketTests, SyncTests}
import cats.effect.laws.util.{TestContext, TestInstances}
import cats.kernel.Eq
import japgolly.scalajs.react.{Callback, CallbackTo, CatsReact}
import cats.tests.CatsSuite
import org.scalacheck.Arbitrary
import japgolly.scalajs.react.CatsReact._

final class CallbackToEffectsSpec extends CatsSuite with TestInstances {
  implicit val ec: TestContext = TestContext()

  trait CallbackToDefer extends Defer[CallbackTo] {
    override def defer[A](fa: => CallbackTo[A]): CallbackTo[A] =
      CallbackTo.byName(fa)
  }

  implicit val callbackToDefer: Defer[CallbackTo] = new CallbackToDefer {}

  implicit val callbackToMonadError: MonadError[CallbackTo, Throwable] =
    CatsReact.reactCallbackCatsInstance
//    implicitly[MonadError[CallbackTo, Throwable]]

  trait CallbackToBracket extends Bracket[CallbackTo, Throwable] {
    override def bracketCase[A, B](acquire: CallbackTo[A])(use: A => CallbackTo[B])(release: (A, ExitCase[Throwable]) => CallbackTo[Unit]): CallbackTo[B] =
      acquire.flatMap { a =>
        handleErrorWith(use(a))(t => release(a, ExitCase.Error(t)).flatMap(_ => raiseError(t)))
          .flatMap(b => release(a, ExitCase.Completed).map(_ => b))
      }
    override def pure[A](x: A): CallbackTo[A] = callbackToMonadError.pure(x)
    override def flatMap[A, B](fa: CallbackTo[A])(f: A => CallbackTo[B]): CallbackTo[B] = callbackToMonadError.flatMap(fa)(f)
    override def tailRecM[A, B](a: A)(f: A => CallbackTo[Either[A, B]]): CallbackTo[B] = callbackToMonadError.tailRecM(a)(f)
    override def raiseError[A](e: Throwable): CallbackTo[A] = callbackToMonadError.raiseError(e)
    override def handleErrorWith[A](fa: CallbackTo[A])(f: Throwable => CallbackTo[A]): CallbackTo[A] = callbackToMonadError.handleErrorWith(fa)(f)
  }

  implicit val callbackToBracket: Bracket[CallbackTo, Throwable] = new CallbackToBracket{}

    trait CallbackToSync extends CallbackToBracket with CallbackToDefer with Sync[CallbackTo] {
    override def suspend[A](thunk: => CallbackTo[A]): CallbackTo[A]
      = CallbackTo.byName(thunk)
  }

  implicit val callbackToSync: Sync[CallbackTo] = new CallbackToSync{}


  implicit def arbitraryCallbackTo[A: Arbitrary]: Arbitrary[CallbackTo[A]] =
    Arbitrary(genApply[A])

  implicit def eqCallback[A](implicit A: Eq[A], ec: TestContext): Eq[CallbackTo[A]] =
    new Eq[CallbackTo[A]] {
      def eqv(x: CallbackTo[A], y: CallbackTo[A]): Boolean =
        eqFuture[A].eqv(x.asAsyncCallback.unsafeToFuture(), y.asAsyncCallback.unsafeToFuture())
    }

  checkAll("Sync[CallbackTo]", SyncTests[CallbackTo].sync[Int, Int, Int])
}
