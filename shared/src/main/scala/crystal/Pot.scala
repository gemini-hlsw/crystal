package crystal

import cats.syntax.all._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

sealed trait Pot[+A] {
  def map[B](f: A => B): Pot[B] =
    this match {
      case pend @ Pending(_) => pend.valueCast[B]
      case err @ Error(_)    => err.valueCast[B]
      case Ready(a)          => Ready(f(a))
    }

  def fold[B](fp: Long => B, fe: Throwable => B, fr: A => B): B =
    this match {
      case Pending(start) => fp(start)
      case Error(t)       => fe(t)
      case Ready(a)       => fr(a)
    }

  def flatten[B](implicit ev: A <:< Pot[B]): Pot[B] =
    this match {
      case pend @ Pending(_) => pend.valueCast[B]
      case err @ Error(_)    => err.valueCast[B]
      case Ready(a)          => a
    }

  def flatMap[B](f: A => Pot[B]): Pot[B] =
    map(f).flatten

  def toOption: Option[A] =
    this match {
      case Ready(a) => a.some
      case _        => none
    }

  def toTryOption: Option[Try[A]] =
    this match {
      case Pending(_) => none
      case Error(t)   => Failure(t).some
      case Ready(a)   => Success(a).some
    }
}

final case class Pending(start: Long = System.currentTimeMillis()) extends Pot[Nothing] {
  def valueCast[B]: Pot[B] = asInstanceOf[Pot[B]]
}
final case class Error(t: Throwable)                               extends Pot[Nothing] {
  def valueCast[B]: Pot[B] = asInstanceOf[Pot[B]]
}
final case class Ready[+A](value: A)                               extends Pot[A]

object Pot {
  def apply[A](a: A): Pot[A] = Ready(a)

  def pending[A]: Pot[A] = Pending()

  def error[A](t: Throwable): Pot[A] = Error(t)

  def fromOption[A](opt: Option[A]): Pot[A] =
    opt match {
      case Some(a) => Ready(a)
      case None    => Pending()
    }

  def fromTry[A](tr: Try[A]): Pot[A] =
    tr match {
      case Success(a) => Ready(a)
      case Failure(t) => Error(t)
    }

  def fromTryOption[A](trOpt: Option[Try[A]]): Pot[A] =
    trOpt match {
      case Some(Success(a)) => Ready(a)
      case Some(Failure(t)) => Error(t)
      case None             => Pending()
    }
}
