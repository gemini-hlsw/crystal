// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.syntax.all._
import monocle.Prism

import scala.util.Failure
import scala.util.Success
import scala.util.Try

sealed trait PotOption[+A] {
  def map[B](f: A => B): PotOption[B] =
    this match {
      case PotOption.Pending        => PotOption.Pending
      case err @ PotOption.Error(_) => err.valueCast[B]
      case PotOption.ReadyNone      => PotOption.ReadyNone
      case PotOption.ReadySome(a)   => PotOption.ReadySome(f(a))
    }

  def fold[B](fp: => B, fe: Throwable => B, frn: => B, frs: A => B): B =
    this match {
      case PotOption.Pending      => fp
      case PotOption.Error(t)     => fe(t)
      case PotOption.ReadyNone    => frn
      case PotOption.ReadySome(a) => frs(a)
    }

  def isReady: Boolean = fold(false, _ => false, true, _ => true)

  def isDefined: Boolean = fold(false, _ => false, false, _ => true)

  def isPending: Boolean = fold(true, _ => false, false, _ => false)

  def isError: Boolean = fold(false, _ => true, false, _ => false)

  def flatten[B](implicit ev: A <:< PotOption[B]): PotOption[B] =
    this match {
      case PotOption.Pending         => PotOption.Pending
      case err @ PotOption.Error(_)  => err.valueCast[B]
      case PotOption.ReadyNone       => PotOption.ReadyNone
      case PotOption.ReadySome(potB) => potB
    }

  def flatMap[B](f: A => PotOption[B]): PotOption[B] =
    map(f).flatten

  def void: PotOption[Unit] = map(_ => ())

  def toPot: Pot[A] = fold(Pot.Pending, Pot.Error.apply, Pot.Pending, Pot.apply)

  def toOption: Option[A] = fold(none, _ => none, none, _.some)

  def toOptionTry: Option[Try[A]] =
    toPot.toOptionTry
}

object PotOption {
  case object Pending                      extends PotOption[Nothing]
  final case class Error(t: Throwable)     extends PotOption[Nothing] {
    def valueCast[B]: PotOption[B] = asInstanceOf[PotOption[B]]
  }
  case object ReadyNone                    extends PotOption[Nothing]
  final case class ReadySome[+A](value: A) extends PotOption[A]

  def apply[A](a: A): PotOption[A] = ReadySome(a)

  def pending[A]: PotOption[A] = Pending

  def error[A](t: Throwable): PotOption[A] = Error(t)

  def readyNone[A]: PotOption[A] = ReadyNone

  def fromPot[A](pot: Pot[A]): PotOption[A] =
    pot.toPotOption

  def fromOption[A](opt: Option[A]): PotOption[A] =
    opt match {
      case Some(a) => ReadySome(a)
      case None    => ReadyNone
    }

  def fromTry[A](tr: Try[A]): PotOption[A] =
    tr match {
      case Success(a) => ReadySome(a)
      case Failure(t) => Error(t)
    }

  def fromOptionTry[A](optTry: Option[Try[A]]): PotOption[A] =
    optTry match {
      case Some(Success(a)) => ReadySome(a)
      case Some(Failure(t)) => Error(t)
      case None             => Pending
    }

  def fromTryOption[A](trOpt: Try[Option[A]]): PotOption[A] =
    trOpt match {
      case Success(Some(a)) => ReadySome(a)
      case Success(None)    => ReadyNone
      case Failure(t)       => Error(t)
    }

  def readySomePrism[A]: Prism[PotOption[A], A] = Prism[PotOption[A], A] {
    case ReadySome(a) => a.some
    case _            => none
  }(apply)

  def errorPrism[A]: Prism[PotOption[A], Throwable] = Prism[PotOption[A], Throwable] {
    case Error(t) => t.some
    case _        => none
  }(Error(_))

}
