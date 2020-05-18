package crystal.data

import scala.annotation.tailrec
import cats.kernel.Eq
import cats.implicits._
import cats.Traverse
import cats.Eval
import cats.Applicative
import cats.MonadError
import cats.Align
import cats.Functor
import cats.data.Ior

sealed trait Pot[A] {
  def map[B](f: A => B): Pot[B] =
    this match {
      case pend @ Pot.Pending(_) => pend.valueCast[B]
      case err @ Pot.Error(_)    => err.valueCast[B]
      case Pot.Ready(a)          => Pot.Ready(f(a))
    }

  def fold[B](fp: Long => B, fe: Throwable => B, fr: A => B): B =
    this match {
      case Pot.Pending(start) => fp(start)
      case Pot.Error(t)       => fe(t)
      case Pot.Ready(a)       => fr(a)
    }

  def flatten[B](implicit ev: A <:< Pot[B]): Pot[B] =
    this match {
      case pend @ Pot.Pending(_) => pend.valueCast[B]
      case err @ Pot.Error(_)    => err.valueCast[B]
      case Pot.Ready(a)          => a
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
  final case class Pending[A](start: Long = System.currentTimeMillis()) extends Pot[A] {
    def valueCast[B]: Pot[B] = asInstanceOf[Pot[B]]
  }
  final case class Error[A](t: Throwable) extends Pot[A] {
    def valueCast[B]: Pot[B] = asInstanceOf[Pot[B]]
  }
  final case class Ready[A](value: A) extends Pot[A]

  def apply[A](a: A): Pot[A] = Ready(a)

  implicit def potEq[A: Eq](implicit eqThrowable: Eq[Throwable]): Eq[Pot[A]] =
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

  implicit object PotCats extends MonadError[Pot, Throwable] with Traverse[Pot] with Align[Pot] {

    override def pure[A](a: A): Pot[A] = Pot(a)

    @tailrec
    override def tailRecM[A, B](a: A)(f: A => Pot[Either[A, B]]): Pot[B] =
      f(a) match {
        case pend @ Pending(_) => pend.valueCast[B]
        case err @ Error(_)    => err.valueCast[B]
        case Ready(Left(a))    => tailRecM(a)(f)
        case Ready(Right(b))   => Ready(b)
      }

    override def flatMap[A, B](fa: Pot[A])(f: A => Pot[B]): Pot[B] =
      fa.flatMap(f)

    override def raiseError[A](t: Throwable): Pot[A] = Error(t)

    override def handleErrorWith[A](fa: Pot[A])(f: Throwable => Pot[A]): Pot[A] =
      fa match {
        case Error(t) => f(t)
        case _        => fa
      }

    override def traverse[F[_], A, B](
      fa: Pot[A]
    )(f:  A => F[B])(implicit F: Applicative[F]): F[Pot[B]] =
      fa match {
        case pend @ Pending(_) => F.pure(pend.valueCast[B])
        case err @ Error(_)    => F.pure(err.valueCast[B])
        case Ready(a)          => F.map(f(a))(Ready(_))
      }

    override def foldLeft[A, B](fa: Pot[A], b: B)(f: (B, A) => B): B =
      fa match {
        case Pending(_) => b
        case Error(_)   => b
        case Ready(a)   => f(b, a)
      }

    override def foldRight[A, B](fa: Pot[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      fa match {
        case Pending(_) => lb
        case Error(_)   => lb
        case Ready(a)   => f(a, lb)
      }

    override def functor: Functor[Pot] = this

    override def align[A, B](fa: Pot[A], fb: Pot[B]): Pot[Ior[A, B]] =
      alignWith(fa, fb)(identity)

    override def alignWith[A, B, C](fa: Pot[A], fb: Pot[B])(f: Ior[A, B] => C): Pot[C] =
      fa match {
        case pend @ Pending(starta) =>
          fb match {
            case Pending(startb) => Pending(math.min(starta, startb))
            case Error(_)        => pend.valueCast[C]
            case Ready(b)        => Ready(f(Ior.right(b)))
          }
        case err @ Error(_)         =>
          fb match {
            case pend @ Pending(_) => pend.valueCast[C]
            case Error(_)          => err.valueCast[C]
            case Ready(b)          => Ready(f(Ior.right(b)))
          }
        case Ready(a)               =>
          fb match {
            case Pending(_) => Ready(f(Ior.left(a)))
            case Error(_)   => Ready(f(Ior.left(a)))
            case Ready(b)   => Ready(f(Ior.both(a, b)))
          }
      }
  }

  implicit final class PotOps[A](private val a: A) extends AnyVal {
    def asReady: Pot[A] = Ready(a)
  }
}
