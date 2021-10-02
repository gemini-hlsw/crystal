package crystal

import cats.syntax.all._
import cats.kernel.Eq
import cats.Align
import cats.Applicative
import cats.Eval
import cats.Functor
import cats.MonadError
import cats.Traverse
import cats.data.Ior
import scala.util.Try
import scala.annotation.tailrec

package object implicits {
  implicit class OptionApplicativeUnitOps[F[_]: Applicative](opt: Option[F[Unit]]) {
    def orUnit: F[Unit] = opt.getOrElse(Applicative[F].unit)
  }

  object throwable {
    // Copied from cats-effect-laws utils.
    implicit val eqThrowable: Eq[Throwable] =
      new Eq[Throwable] {
        def eqv(x: Throwable, y: Throwable): Boolean =
          // All exceptions are non-terminating and given exceptions
          // aren't values (being mutable, they implement reference
          // equality), then we can't really test them reliably,
          // especially due to race conditions or outside logic
          // that wraps them (e.g. ExecutionException)
          (x ne null) == (y ne null)
      }
  }

  implicit final class AnyToPotOps[A](private val a: A) extends AnyVal {
    def ready: Pot[A] = Ready(a)
  }

  implicit final class AnyOptionToPotOps[A](private val a: Option[A]) extends AnyVal {
    def toPot: Pot[A] = Pot.fromOption(a)
  }

  implicit final class TryOptionToPotOps[A](private val a: Option[Try[A]]) extends AnyVal {
    def toPot: Pot[A] = Pot.fromTryOption(a)
  }

  implicit final class TryToPotOps[A](private val a: Try[A]) /* extends AnyVal */ {
    def toPot: Pot[A] = Pot.fromTry[A](a)
  }

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

}
