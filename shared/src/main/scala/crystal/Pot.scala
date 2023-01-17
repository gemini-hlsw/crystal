// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.syntax.all._
import crystal.implicits._
import monocle.Prism

import scala.util.Failure
import scala.util.Success
import scala.util.Try

sealed trait Pot[+A] {
  def map[B](f: A => B): Pot[B] =
    this match {
      case Pot.Pending        => Pot.Pending
      case err @ Pot.Error(_) => err.valueCast[B]
      case Pot.Ready(a)       => Pot.Ready(f(a))
    }

  def fold[B](fp: => B, fe: Throwable => B, fr: A => B): B =
    this match {
      case Pot.Pending  => fp
      case Pot.Error(t) => fe(t)
      case Pot.Ready(a) => fr(a)
    }

  def isReady: Boolean = fold(false, _ => false, _ => true)

  def isPending: Boolean = fold(true, _ => false, _ => false)

  def isError: Boolean = fold(false, _ => true, _ => false)

  def flatten[B](implicit ev: A <:< Pot[B]): Pot[B] =
    this match {
      case Pot.Pending        => Pot.Pending
      case err @ Pot.Error(_) => err.valueCast[B]
      case Pot.Ready(potB)    => potB
    }

  def flatMap[B](f: A => Pot[B]): Pot[B] =
    map(f).flatten

  def void: Pot[Unit] = map(_ => ())

  def toPotOption: PotOption[A] =
    fold(PotOption.Pending, PotOption.Error.apply, _.readySome)

  def toOption: Option[A] = fold(none, _ => none, _.some)

  def toOptionTry: Option[Try[A]] =
    this match {
      case Pot.Pending  => none
      case Pot.Error(t) => Failure(t).some
      case Pot.Ready(a) => Success(a).some
    }
}

object Pot {
  case object Pending                  extends Pot[Nothing]
  final case class Error(t: Throwable) extends Pot[Nothing] {
    def valueCast[B]: Pot[B] = asInstanceOf[Pot[B]]
  }
  final case class Ready[+A](value: A) extends Pot[A]

  def apply[A](a: A): Pot[A] = Ready(a)

  def pending[A]: Pot[A] = Pending

  def error[A](t: Throwable): Pot[A] = Error(t)

  def fromOption[A](po: PotOption[A]): Pot[A] =
    po.toPot

  def fromOption[A](opt: Option[A]): Pot[A] =
    opt match {
      case Some(a) => Ready(a)
      case None    => Pending
    }

  def fromTry[A](tr: Try[A]): Pot[A] =
    tr match {
      case Success(a) => Ready(a)
      case Failure(t) => Error(t)
    }

  def fromOptionTry[A](trOpt: Option[Try[A]]): Pot[A] =
    trOpt match {
      case Some(Success(a)) => Ready(a)
      case Some(Failure(t)) => Error(t)
      case None             => Pending
    }

  def readyPrism[A]: Prism[Pot[A], A] = Prism[Pot[A], A] {
    case Ready(a) => a.some
    case _        => none
  }(apply)

  def errorPrism[A]: Prism[Pot[A], Throwable] = Prism[Pot[A], Throwable] {
    case Error(t) => t.some
    case _        => none
  }(Error(_))
}
