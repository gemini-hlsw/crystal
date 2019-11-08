package com.rpiaggio.crystal.effects

import cats.effect.{Bracket, ExitCase, Sync}
import cats.{Defer, MonadError}
import japgolly.scalajs.react.{AsyncCallback, CatsReact}

trait AsyncCallbackEffects {
  private val asyncCallbackMonadError: MonadError[AsyncCallback, Throwable] =
    CatsReact.reactAsyncCallbackCatsInstance
  //      implicitly[MonadError[AsyncCallback, Throwable]]

  trait AsyncCallbackDefer extends Defer[AsyncCallback] {
    override def defer[A](fa: => AsyncCallback[A]): AsyncCallback[A] =
      AsyncCallback.byName(fa)
  }

  implicit val asyncCallbackDefer: Defer[AsyncCallback] = new AsyncCallbackDefer {}

  trait AsyncCallbackBracket extends Bracket[AsyncCallback, Throwable] {
    override def bracketCase[A, B](acquire: AsyncCallback[A])(use: A => AsyncCallback[B])(release: (A, ExitCase[Throwable]) => AsyncCallback[Unit]): AsyncCallback[B] =
      acquire.flatMap { a =>
        handleErrorWith(use(a))(t => release(a, ExitCase.Error(t)).flatMap(_ => raiseError(t)))
          .flatMap(b => release(a, ExitCase.Completed).map(_ => b))
      }
    override def pure[A](x: A): AsyncCallback[A] = asyncCallbackMonadError.pure(x)
    override def flatMap[A, B](fa: AsyncCallback[A])(f: A => AsyncCallback[B]): AsyncCallback[B] = asyncCallbackMonadError.flatMap(fa)(f)
    override def tailRecM[A, B](a: A)(f: A => AsyncCallback[Either[A, B]]): AsyncCallback[B] = asyncCallbackMonadError.tailRecM(a)(f)
    override def raiseError[A](e: Throwable): AsyncCallback[A] = asyncCallbackMonadError.raiseError(e)
    override def handleErrorWith[A](fa: AsyncCallback[A])(f: Throwable => AsyncCallback[A]): AsyncCallback[A] = asyncCallbackMonadError.handleErrorWith(fa)(f)
  }

  implicit val asyncCallbackBracket: Bracket[AsyncCallback, Throwable] = new AsyncCallbackBracket {}

  trait AsyncCallbackSync extends AsyncCallbackBracket with AsyncCallbackDefer with Sync[AsyncCallback] {
    override def suspend[A](thunk: => AsyncCallback[A]): AsyncCallback[A] =
      AsyncCallback.byName(thunk)
  }

  implicit val asyncCallbackSync: Sync[AsyncCallback] = new AsyncCallbackSync {}
}

object AsyncCallbackEffects extends AsyncCallbackEffects

