package crystal

import cats.implicits._
import monocle.Lens
import monocle.Optional
import monocle.Prism
import scala.concurrent.Promise
import cats.effect.ContextShift
import cats.effect.Async
import cats.effect.Sync

class ViewOptF[F[_], A](
  value:      Option[A],
  val modOpt: (Option[A] => Option[A]) => F[Unit] // Shouldn't actually be available in ViewF
) {
  @inline def getOption: Option[A] = value

  @inline def mod(f: A => A): F[Unit] = modOpt(_.map(f))

  @inline def set(a: A): F[Unit] = modOpt(_ => a.some)

  @inline def setOpt(a: Option[A]): F[Unit] =
    modOpt(_ => a) // Shouldn't actually be available in ViewF

  def modOptAndGetOption( // Shouldn't actually be available in ViewF
    f:           Option[A] => Option[A]
  )(implicit ev: Async[F], cs: ContextShift[F]): F[Option[A]] =
    Sync[F].delay(Promise[Option[A]]).flatMap { p =>
      modOpt { a: Option[A] =>
        val fa = f(a)
        p.success(fa)
        fa
      } >> Async.fromFuture(Sync[F].delay(p.future))
    }

  def modAndGetOption(f: A => A)(implicit ev: Async[F], cs: ContextShift[F]): F[Option[A]] =
    modOptAndGetOption(_.map(f))

  def zoom[B](get: A => B)(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(
      value.map(get),
      (f: Option[B] => Option[B]) => mod(modB(b => f(b.some).getOrElse(b)))
    )

  def zoomOpt[B](
    get:  A => Option[B]
  )(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(
      value.flatMap(get),
      (f: Option[B] => Option[B]) => mod(modB(b => f(b.some).getOrElse(b)))
    )

  def zoomOptOpt[B](
    get:  A => Option[B]
  )(modB: (Option[B] => Option[B]) => Option[A] => Option[A]): ViewOptF[F, B] =
    new ViewOptF(
      value.flatMap(get),
      (f: Option[B] => Option[B]) => modOpt(modB(f))
    )

  def zoomL[B](lens: Lens[A, B]): ViewOptF[F, B] =
    zoom(lens.get)(lens.modify)

  def zoomO[B](optional: Optional[A, B]): ViewOptF[F, B] =
    zoomOpt(optional.getOption)(optional.modify)

  def zoomP[B](prism: Prism[A, B]): ViewOptF[F, B] =
    zoomOpt(prism.getOption)(prism.modify)

  def withOnModOpt(
    f:           Option[A] => F[Unit]
  )(implicit ev: Async[F], cs: ContextShift[F]): ViewOptF[F, A] =
    new ViewOptF[F, A](value, modF => modOptAndGetOption(modF).flatMap(f))

  override def toString(): String = s"ViewOptF($value, <modFn>)"
}

object ViewOptF {
  def apply[F[_], A](
    value:  Option[A],
    modOpt: (Option[A] => Option[A]) => F[Unit]
  ): ViewOptF[F, A] =
    new ViewOptF(value, modOpt)
}

// The difference between a View and a StateSnapshot is that the modifier doesn't act on the current value,
// but passes the modifier function to an external source of truth. Since we are defining no getter
// from such source of truth, a View is defined in terms of a modifier function instead of a setter.
//
// If modOpt, setOpt or modOptAndGetOption are called with a resulting None, it's interpreted as
// "don't change the current value", thus ensuring there's always an A.
// We should restructure things though so that those methods aren't available in ViewF.
class ViewF[F[_], A](value: A, mod: (A => A) => F[Unit])
    extends ViewOptF[F, A](
      value.some,
      (f: Option[A] => Option[A]) => mod(a => f(a.some).getOrElse(a))
    ) {
  @inline def get: A = value

  def modAndGet(f: A => A)(implicit ev: Async[F], cs: ContextShift[F]): F[A] =
    modAndGetOption(f).map(
      _.get
    ) // There's logic to ensure that the result is never None, but we should probably restructure things to avoid doing a .get.

  override def zoom[B](get:   A => B)(modB: (B => B) => A => A): ViewF[F, B] =
    new ViewF(get(value), (f: B => B) => mod(modB(f)))

  override def zoomL[B](lens: Lens[A, B]): ViewF[F, B] =
    zoom(lens.get)(lens.modify)

  def withOnMod(
    f:           A => F[Unit]
  )(implicit ev: Async[F], cs: ContextShift[F]): ViewF[F, A] =
    new ViewF[F, A](value, modF => modAndGet(modF).flatMap(f))

  override def toString(): String = s"ViewF($value, <modFn>)"
}

object ViewF {
  def apply[F[_], A](value: A, mod: (A => A) => F[Unit]): ViewF[F, A] =
    new ViewF(value, mod)
}
