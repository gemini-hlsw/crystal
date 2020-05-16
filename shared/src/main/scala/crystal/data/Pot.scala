package crystal.data

import cats.Monad
import scala.annotation.tailrec
import cats.kernel.Eq
import cats.implicits._
import crystal.data.implicits._

sealed trait Pot[A] {
  def map[B](f: A => B): Pot[B] =
    this match {
      case Pot.Pending(start) => Pot.Pending(start)
      case Pot.Error(t)       => Pot.Error(t)
      case Pot.Ready(a)       => Pot.Ready(f(a))
    }

  def fold[B](fp: Long => B, fe: Throwable => B, fr: A => B): B =
    this match {
      case Pot.Pending(start) => fp(start)
      case Pot.Error(t)       => fe(t)
      case Pot.Ready(a)       => fr(a)
    }

  def flatten[B](implicit ev: A <:< Pot[B]): Pot[B] =
    this match {
      case Pot.Pending(start) => Pot.Pending(start)
      case Pot.Error(t)       => Pot.Error(t)
      case Pot.Ready(a)       => a
    }

  def flatMap[B](f: A => Pot[B]): Pot[B] =
    map(f).flatten

  def toOption: Option[A] =
    this match {
      case Pot.Ready(a) => a.some
      case _            => none
    }
}

object Pot {
  final case class Pending[A](start: Long = System.currentTimeMillis()) extends Pot[A]
  final case class Error[A](t: Throwable) extends Pot[A]
  final case class Ready[A](value: A) extends Pot[A]

  def apply[A](a: A): Pot[A] = Ready(a)

  implicit def potEq[A: Eq]: Eq[Pot[A]] =
    new Eq[Pot[A]] {
      def eqv(x: Pot[A], y: Pot[A]): Boolean =
        x match {
          case Pending(startx) =>
            y match {
              case Pending(starty) => startx === starty
              case _               => false
            }
          case Error(tx)       =>
            y match {
              case Error(ty) => tx === ty
              case _         => false
            }
          case Ready(ax)       =>
            y match {
              case Ready(ay) => ax === ay
              case _         => false
            }
        }
    }

  implicit object PotMonad extends Monad[Pot] {
    override def pure[A](a: A): Pot[A] = Pot(a)

    @tailrec
    override def tailRecM[A, B](a: A)(f: A => Pot[Either[A, B]]): Pot[B] =
      f(a) match {
        case Pending(start)  => Pending(start)
        case Error(t)        => Error(t)
        case Ready(Left(a))  => tailRecM(a)(f)
        case Ready(Right(b)) => Ready(b)
      }

    override def flatMap[A, B](fa: Pot[A])(f: A => Pot[B]): Pot[B] =
      fa.flatMap(f)
  }

}
