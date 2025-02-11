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
import crystal.*
import monocle.Prism

import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try

sealed trait Pot[+A]:
  def map[B](f: A => B): Pot[B] =
    this match
      case Pot.Pending        => Pot.Pending
      case err @ Pot.Error(_) => err.valueCast[B]
      case Pot.Ready(a)       => Pot.Ready(f(a))

  def fold[B](fp: => B, fe: Throwable => B, fr: A => B): B =
    this match
      case Pot.Pending  => fp
      case Pot.Error(t) => fe(t)
      case Pot.Ready(a) => fr(a)

  def isReady: Boolean = fold(false, _ => false, _ => true)

  def isPending: Boolean = fold(true, _ => false, _ => false)

  def isError: Boolean = fold(false, _ => true, _ => false)

  def flatten[B](using ev: A <:< Pot[B]): Pot[B] =
    this match
      case Pot.Pending        => Pot.Pending
      case err @ Pot.Error(_) => err.valueCast[B]
      case Pot.Ready(potB)    => ev(potB)

  def flatMap[B](f: A => Pot[B]): Pot[B] =
    map(f).flatten

  def void: Pot[Unit] = map(_ => ())

  def toPotOption: PotOption[A] =
    fold(PotOption.Pending, PotOption.Error.apply, _.readySome)

  def toOption: Option[A] = fold(none, _ => none, _.some)

  def toOptionTry: Option[Try[A]] =
    this match
      case Pot.Pending  => none
      case Pot.Error(t) => Failure(t).some
      case Pot.Ready(a) => Success(a).some

  def toOptionEither: Option[Either[Throwable, A]] = toOptionTry.map(_.toEither)

  def filter(f: A => Boolean): Pot[A] =
    this match
      case Pot.Ready(a) if !f(a) => Pot.pending
      case _                     => this

  def filterNot(f: A => Boolean): Pot[A] = filter(a => !f(a))

object Pot:
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
    opt match
      case Some(a) => Ready(a)
      case None    => Pending

  def fromTry[A](tr: Try[A]): Pot[A] =
    tr match
      case Success(a) => Ready(a)
      case Failure(t) => Error(t)

  def fromOptionTry[A](trOpt: Option[Try[A]]): Pot[A] =
    trOpt match
      case Some(Success(a)) => Ready(a)
      case Some(Failure(t)) => Error(t)
      case None             => Pending

  def readyPrism[A]: Prism[Pot[A], A] = Prism[Pot[A], A] {
    case Ready(a) => a.some
    case _        => none
  }(apply)

  def errorPrism[A]: Prism[Pot[A], Throwable] = Prism[Pot[A], Throwable] {
    case Error(t) => t.some
    case _        => none
  }(Error(_))

  given Eq[Pot[Nothing]] =
    new Eq[Pot[Nothing]] {
      import throwable.given

      def eqv(x: Pot[Nothing], y: Pot[Nothing]): Boolean =
        x match
          case Pot.Pending   =>
            y match
              case Pot.Pending => true
              case _           => false
          case Pot.Error(tx) =>
            y match
              case Pot.Error(ty) => tx === ty
              case _             => false
    }

  given [A: Eq]: Eq[Pot[A]] =
    new Eq[Pot[A]] {
      def eqv(x: Pot[A], y: Pot[A]): Boolean =
        x match
          case Pot.Ready(ax) =>
            y match
              case Pot.Ready(ay) => ax === ay
              case _             => false
          case _             =>
            y match
              case Pot.Ready(_) => false
              case _            => x.asInstanceOf[Pot[Nothing]] === y.asInstanceOf[Pot[Nothing]]
    }

  implicit object PotCats extends MonadError[Pot, Throwable], Traverse[Pot], Align[Pot] {

    override def pure[A](a: A): Pot[A] = Pot(a)

    @tailrec
    override def tailRecM[A, B](a: A)(f: A => Pot[Either[A, B]]): Pot[B] =
      f(a) match
        case Pot.Pending         => Pot.Pending
        case err @ Pot.Error(_)  => err.valueCast[B]
        case Pot.Ready(Left(a))  => tailRecM(a)(f)
        case Pot.Ready(Right(b)) => Pot.Ready(b)

    override def flatMap[A, B](fa: Pot[A])(f: A => Pot[B]): Pot[B] =
      fa.flatMap(f)

    override def raiseError[A](t: Throwable): Pot[A] = Pot.Error(t)

    override def handleErrorWith[A](fa: Pot[A])(f: Throwable => Pot[A]): Pot[A] =
      fa match
        case Pot.Error(t) => f(t)
        case _            => fa

    override def traverse[F[_], A, B](
      fa: Pot[A]
    )(f: A => F[B])(using F: Applicative[F]): F[Pot[B]] =
      fa match
        case Pot.Pending        => F.pure(Pot.Pending)
        case err @ Pot.Error(_) => F.pure(err.valueCast[B])
        case Pot.Ready(a)       => F.map(f(a))(Pot.Ready(_))

    override def foldLeft[A, B](fa: Pot[A], b: B)(f: (B, A) => B): B =
      fa match
        case Pot.Pending  => b
        case Pot.Error(_) => b
        case Pot.Ready(a) => f(b, a)

    override def foldRight[A, B](fa: Pot[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      fa match
        case Pot.Pending  => lb
        case Pot.Error(_) => lb
        case Pot.Ready(a) => f(a, lb)

    override def functor: Functor[Pot] = this

    override def align[A, B](fa: Pot[A], fb: Pot[B]): Pot[Ior[A, B]] =
      alignWith(fa, fb)(identity)

    override def alignWith[A, B, C](fa: Pot[A], fb: Pot[B])(f: Ior[A, B] => C): Pot[C] =
      fa match
        case Pot.Pending         =>
          fb match
            case Pot.Pending         => Pot.Pending
            case errb @ Pot.Error(_) => errb.valueCast[C]
            case Pot.Ready(b)        => Pot.Ready(f(Ior.right(b)))
        case erra @ Pot.Error(_) =>
          fb match
            case Pot.Ready(b) => Pot.Ready(f(Ior.right(b)))
            case _            => erra.valueCast[C]
        case Pot.Ready(a)        =>
          fb match
            case Pot.Ready(b) => Pot.Ready(f(Ior.both(a, b)))
            case _            => Pot.Ready(f(Ior.left(a)))
  }
