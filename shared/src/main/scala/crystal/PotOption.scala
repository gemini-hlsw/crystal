// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Align
import cats.Applicative
import cats.Eq
import cats.Eval
import cats.Functor
import cats.MonadError
import cats.Traverse
import cats.data.Ior
import cats.syntax.all.*
import monocle.Prism

import scala.annotation.tailrec
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

  def flatten[B](using A <:< PotOption[B]): PotOption[B] =
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

  given Eq[PotOption[Nothing]] =
    new Eq[PotOption[Nothing]] {
      import throwable.given

      def eqv(x: PotOption[Nothing], y: PotOption[Nothing]): Boolean =
        x match
          case PotOption.Pending   =>
            y match
              case PotOption.Pending => true
              case _                 => false
          case PotOption.Error(tx) =>
            y match
              case PotOption.Error(ty) => tx === ty
              case _                   => false
          case PotOption.ReadyNone =>
            y match
              case PotOption.ReadyNone => true
              case _                   => false
          case _                   => false
    }

  given [A: Eq]: Eq[PotOption[A]] =
    new Eq[PotOption[A]] {
      def eqv(x: PotOption[A], y: PotOption[A]): Boolean =
        x match
          case PotOption.ReadySome(ax) =>
            y match
              case PotOption.ReadySome(ay) => ax === ay
              case _                       => false
          case _                       =>
            y match
              case PotOption.ReadySome(_) => false
              case _                      => x.asInstanceOf[PotOption[Nothing]] === y.asInstanceOf[PotOption[Nothing]]
    }

  implicit object PotOptionCats
      extends MonadError[PotOption, Throwable],
        Traverse[PotOption],
        Align[PotOption] {

    override def pure[A](a: A): PotOption[A] = PotOption(a)

    @tailrec
    override def tailRecM[A, B](a: A)(f: A => PotOption[Either[A, B]]): PotOption[B] =
      f(a) match
        case PotOption.Pending             => PotOption.Pending
        case err @ PotOption.Error(_)      => err.valueCast[B]
        case PotOption.ReadyNone           => PotOption.ReadyNone
        case PotOption.ReadySome(Left(a))  => tailRecM(a)(f)
        case PotOption.ReadySome(Right(b)) => PotOption.ReadySome(b)

    override def flatMap[A, B](fa: PotOption[A])(f: A => PotOption[B]): PotOption[B] =
      fa.flatMap(f)

    override def raiseError[A](t: Throwable): PotOption[A] = PotOption.Error(t)

    override def handleErrorWith[A](fa: PotOption[A])(f: Throwable => PotOption[A]): PotOption[A] =
      fa match
        case PotOption.Error(t) => f(t)
        case _                  => fa

    override def traverse[F[_], A, B](
      fa: PotOption[A]
    )(f: A => F[B])(using F: Applicative[F]): F[PotOption[B]] =
      fa match
        case PotOption.Pending        => F.pure(PotOption.Pending)
        case err @ PotOption.Error(_) => F.pure(err.valueCast[B])
        case PotOption.ReadyNone      => F.pure(PotOption.ReadyNone)
        case PotOption.ReadySome(a)   => F.map(f(a))(PotOption.ReadySome(_))

    override def foldLeft[A, B](fa: PotOption[A], b: B)(f: (B, A) => B): B =
      fa match
        case PotOption.Pending      => b
        case PotOption.Error(_)     => b
        case PotOption.ReadyNone    => b
        case PotOption.ReadySome(a) => f(b, a)

    override def foldRight[A, B](fa: PotOption[A], lb: Eval[B])(
      f: (A, Eval[B]) => Eval[B]
    ): Eval[B] =
      fa match
        case PotOption.Pending      => lb
        case PotOption.Error(_)     => lb
        case PotOption.ReadyNone    => lb
        case PotOption.ReadySome(a) => f(a, lb)

    override def functor: Functor[PotOption] = this

    override def align[A, B](fa: PotOption[A], fb: PotOption[B]): PotOption[Ior[A, B]] =
      alignWith(fa, fb)(identity)

    override def alignWith[A, B, C](fa: PotOption[A], fb: PotOption[B])(
      f: Ior[A, B] => C
    ): PotOption[C] =
      fa match
        case PotOption.Pending         =>
          fb match
            case PotOption.ReadySome(b)    => PotOption.ReadySome(f(Ior.right(b)))
            case errb @ PotOption.Error(_) => errb.valueCast[C]
            case _                         => PotOption.Pending
        case erra @ PotOption.Error(_) =>
          fb match
            case PotOption.ReadySome(b) => PotOption.ReadySome(f(Ior.right(b)))
            case _                      => erra.valueCast[C]
        case PotOption.ReadyNone       =>
          fb match
            case PotOption.Pending         => PotOption.Pending
            case errb @ PotOption.Error(_) => errb.valueCast[C]
            case PotOption.ReadyNone       => PotOption.ReadyNone
            case PotOption.ReadySome(b)    => PotOption.ReadySome(f(Ior.right(b)))
        case PotOption.ReadySome(a)    =>
          fb match
            case PotOption.ReadySome(b) => PotOption.ReadySome(f(Ior.both(a, b)))
            case _                      => PotOption.ReadySome(f(Ior.left(a)))
  }
}
