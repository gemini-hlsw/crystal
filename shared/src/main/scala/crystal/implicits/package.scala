// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Align
import cats.Applicative
import cats.Eval
import cats.Functor
import cats.MonadError
import cats.Traverse
import cats.data.Ior
import cats.kernel.Eq
import cats.syntax.all._

import scala.annotation.tailrec
import scala.util.Try

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
    def ready: Pot[A] = Pot.Ready(a)

    def readySome: PotOption[A] = PotOption.ReadySome(a)
  }

  implicit final class AnyOptionToPotOps[A](private val a: Option[A]) extends AnyVal {
    def toPot: Pot[A] = Pot.fromOption(a)

    def toPotOption: PotOption[A] = PotOption.fromOption(a)
  }

  implicit final class TryOptionToPotOps[A](private val a: Option[Try[A]]) extends AnyVal {
    def toPot: Pot[A] = Pot.fromOptionTry(a)

    def toPotOption: PotOption[A] = PotOption.fromOptionTry(a)
  }

  implicit final class TryToPotOps[A](private val a: Try[A]) extends AnyVal {
    def toPot: Pot[A] = Pot.fromTry[A](a)

    def toPotOption: PotOption[A] = PotOption.fromTry(a)
  }

  implicit val eqPotNothing: Eq[Pot[Nothing]] =
    new Eq[Pot[Nothing]] {
      import throwable._

      def eqv(x: Pot[Nothing], y: Pot[Nothing]): Boolean =
        x match {
          case Pot.Pending   =>
            y match {
              case Pot.Pending => true
              case _           => false
            }
          case Pot.Error(tx) =>
            y match {
              case Pot.Error(ty) => tx === ty
              case _             => false
            }
          case _             => false
        }
    }

  implicit def eqPot[A: Eq]: Eq[Pot[A]] =
    new Eq[Pot[A]] {
      def eqv(x: Pot[A], y: Pot[A]): Boolean =
        x match {
          case Pot.Ready(ax) =>
            y match {
              case Pot.Ready(ay) => ax === ay
              case _             => false
            }
          case _             =>
            y match {
              case Pot.Ready(_) => false
              case _            => eqPotNothing.eqv(x.asInstanceOf[Pot[Nothing]], y.asInstanceOf[Pot[Nothing]])
            }
        }
    }

  implicit object PotCats extends MonadError[Pot, Throwable] with Traverse[Pot] with Align[Pot] {

    override def pure[A](a: A): Pot[A] = Pot(a)

    @tailrec
    override def tailRecM[A, B](a: A)(f: A => Pot[Either[A, B]]): Pot[B] =
      f(a) match {
        case Pot.Pending         => Pot.Pending
        case err @ Pot.Error(_)  => err.valueCast[B]
        case Pot.Ready(Left(a))  => tailRecM(a)(f)
        case Pot.Ready(Right(b)) => Pot.Ready(b)
      }

    override def flatMap[A, B](fa: Pot[A])(f: A => Pot[B]): Pot[B] =
      fa.flatMap(f)

    override def raiseError[A](t: Throwable): Pot[A] = Pot.Error(t)

    override def handleErrorWith[A](fa: Pot[A])(f: Throwable => Pot[A]): Pot[A] =
      fa match {
        case Pot.Error(t) => f(t)
        case _            => fa
      }

    override def traverse[F[_], A, B](
      fa: Pot[A]
    )(f:  A => F[B])(implicit F: Applicative[F]): F[Pot[B]] =
      fa match {
        case Pot.Pending        => F.pure(Pot.Pending)
        case err @ Pot.Error(_) => F.pure(err.valueCast[B])
        case Pot.Ready(a)       => F.map(f(a))(Pot.Ready(_))
      }

    override def foldLeft[A, B](fa: Pot[A], b: B)(f: (B, A) => B): B =
      fa match {
        case Pot.Pending  => b
        case Pot.Error(_) => b
        case Pot.Ready(a) => f(b, a)
      }

    override def foldRight[A, B](fa: Pot[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      fa match {
        case Pot.Pending  => lb
        case Pot.Error(_) => lb
        case Pot.Ready(a) => f(a, lb)
      }

    override def functor: Functor[Pot] = this

    override def align[A, B](fa: Pot[A], fb: Pot[B]): Pot[Ior[A, B]] =
      alignWith(fa, fb)(identity)

    override def alignWith[A, B, C](fa: Pot[A], fb: Pot[B])(f: Ior[A, B] => C): Pot[C] =
      fa match {
        case Pot.Pending         =>
          fb match {
            case Pot.Pending         => Pot.Pending
            case errb @ Pot.Error(_) => errb.valueCast[C]
            case Pot.Ready(b)        => Pot.Ready(f(Ior.right(b)))
          }
        case erra @ Pot.Error(_) =>
          fb match {
            case Pot.Ready(b) => Pot.Ready(f(Ior.right(b)))
            case _            => erra.valueCast[C]
          }
        case Pot.Ready(a)        =>
          fb match {
            case Pot.Ready(b) => Pot.Ready(f(Ior.both(a, b)))
            case _            => Pot.Ready(f(Ior.left(a)))
          }
      }
  }

  implicit val eqPotOptionNothing: Eq[PotOption[Nothing]] =
    new Eq[PotOption[Nothing]] {
      import throwable._

      def eqv(x: PotOption[Nothing], y: PotOption[Nothing]): Boolean =
        x match {
          case PotOption.Pending   =>
            y match {
              case PotOption.Pending => true
              case _                 => false
            }
          case PotOption.Error(tx) =>
            y match {
              case PotOption.Error(ty) => tx === ty
              case _                   => false
            }
          case PotOption.ReadyNone =>
            y match {
              case PotOption.ReadyNone => true
              case _                   => false
            }

          case _ => false
        }
    }

  implicit def eqPotOption[A: Eq]: Eq[PotOption[A]] =
    new Eq[PotOption[A]] {
      def eqv(x: PotOption[A], y: PotOption[A]): Boolean =
        x match {
          case PotOption.ReadySome(ax) =>
            y match {
              case PotOption.ReadySome(ay) => ax === ay
              case _                       => false
            }
          case _                       =>
            y match {
              case PotOption.ReadySome(_) => false
              case _                      =>
                eqPotOptionNothing.eqv(
                  x.asInstanceOf[PotOption[Nothing]],
                  y.asInstanceOf[PotOption[Nothing]]
                )
            }
        }
    }

  implicit object PotOptionCats
      extends MonadError[PotOption, Throwable]
      with Traverse[PotOption]
      with Align[PotOption] {

    override def pure[A](a: A): PotOption[A] = PotOption(a)

    @tailrec
    override def tailRecM[A, B](a: A)(f: A => PotOption[Either[A, B]]): PotOption[B] =
      f(a) match {
        case PotOption.Pending             => PotOption.Pending
        case err @ PotOption.Error(_)      => err.valueCast[B]
        case PotOption.ReadyNone           => PotOption.ReadyNone
        case PotOption.ReadySome(Left(a))  => tailRecM(a)(f)
        case PotOption.ReadySome(Right(b)) => PotOption.ReadySome(b)
      }

    override def flatMap[A, B](fa: PotOption[A])(f: A => PotOption[B]): PotOption[B] =
      fa.flatMap(f)

    override def raiseError[A](t: Throwable): PotOption[A] = PotOption.Error(t)

    override def handleErrorWith[A](fa: PotOption[A])(f: Throwable => PotOption[A]): PotOption[A] =
      fa match {
        case PotOption.Error(t) => f(t)
        case _                  => fa
      }

    override def traverse[F[_], A, B](
      fa: PotOption[A]
    )(f:  A => F[B])(implicit F: Applicative[F]): F[PotOption[B]] =
      fa match {
        case PotOption.Pending        => F.pure(PotOption.Pending)
        case err @ PotOption.Error(_) => F.pure(err.valueCast[B])
        case PotOption.ReadyNone      => F.pure(PotOption.ReadyNone)
        case PotOption.ReadySome(a)   => F.map(f(a))(PotOption.ReadySome(_))
      }

    override def foldLeft[A, B](fa: PotOption[A], b: B)(f: (B, A) => B): B =
      fa match {
        case PotOption.Pending      => b
        case PotOption.Error(_)     => b
        case PotOption.ReadyNone    => b
        case PotOption.ReadySome(a) => f(b, a)
      }

    override def foldRight[A, B](fa: PotOption[A], lb: Eval[B])(
      f:                             (A, Eval[B]) => Eval[B]
    ): Eval[B] =
      fa match {
        case PotOption.Pending      => lb
        case PotOption.Error(_)     => lb
        case PotOption.ReadyNone    => lb
        case PotOption.ReadySome(a) => f(a, lb)
      }

    override def functor: Functor[PotOption] = this

    override def align[A, B](fa: PotOption[A], fb: PotOption[B]): PotOption[Ior[A, B]] =
      alignWith(fa, fb)(identity)

    override def alignWith[A, B, C](fa: PotOption[A], fb: PotOption[B])(
      f:                                Ior[A, B] => C
    ): PotOption[C] =
      fa match {
        case PotOption.Pending         =>
          fb match {
            case PotOption.ReadySome(b)    => PotOption.ReadySome(f(Ior.right(b)))
            case errb @ PotOption.Error(_) => errb.valueCast[C]
            case _                         => PotOption.Pending
          }
        case erra @ PotOption.Error(_) =>
          fb match {
            case PotOption.ReadySome(b) => PotOption.ReadySome(f(Ior.right(b)))
            case _                      => erra.valueCast[C]
          }
        case PotOption.ReadyNone       =>
          fb match {
            case PotOption.Pending         => PotOption.Pending
            case errb @ PotOption.Error(_) => errb.valueCast[C]
            case PotOption.ReadyNone       => PotOption.ReadyNone
            case PotOption.ReadySome(b)    => PotOption.ReadySome(f(Ior.right(b)))
          }
        case PotOption.ReadySome(a)    =>
          fb match {
            case PotOption.ReadySome(b) => PotOption.ReadySome(f(Ior.both(a, b)))
            case _                      => PotOption.ReadySome(f(Ior.left(a)))
          }
      }
  }

}
