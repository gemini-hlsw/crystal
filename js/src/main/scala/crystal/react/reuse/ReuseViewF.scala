package crystal.react.reuse

import cats.Id
import cats.Monad
import cats.Monoid
import cats.effect.Async
import cats.syntax.all._
import crystal.ViewF
import crystal.ViewListF
import crystal.ViewOps
import crystal.ViewOptF
import japgolly.scalajs.react.Reusability
import monocle.Iso
import monocle.Lens
import monocle.Optional
import monocle.Prism
import monocle.Traversal

class ReuseViewF[F[_]: Monad, A](val rv: Reuse[ViewF[F, A]]) extends ViewOps[F, Id, A] {
  val get: A                                           = rv.value.get
  val modCB: ((A => A), A => F[Unit]) => F[Unit]       = rv.value.modCB
  def modAndGet(f: A => A)(implicit F: Async[F]): F[A] = rv.value.modAndGet(f)

  def map[B](f: ViewF[F, A] => ViewF[F, B]): ReuseViewF[F, B] =
    ReuseViewF[F, B](rv.map(f))

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ReuseViewF[F, B] = map(_.zoom(getB)(modB))

  def zoomOpt[B](getB: A => Option[B])(modB: (B => B) => A => A): ReuseViewOptF[F, B] =
    ReuseViewOptF(rv.map(_.zoomOpt(getB)(modB)))

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ReuseViewListF[F, B] = ReuseViewListF(
    rv.map(_.zoomList(getB)(modB))
  )

  def as[B](iso: Iso[A, B]): ReuseViewF[F, B] = zoom(iso.asLens)

  def asOpt: ReuseViewOptF[F, A] = zoom(Iso.id[A].asOptional)

  def asList: ReuseViewListF[F, A] = zoom(Iso.id[A].asTraversal)

  def zoom[B](lens: Lens[A, B]): ReuseViewF[F, B] =
    zoom(lens.get _)(lens.modify)

  def zoom[B](optional: Optional[A, B]): ReuseViewOptF[F, B] =
    zoomOpt(optional.getOption _)(optional.modify)

  def zoom[B](prism: Prism[A, B]): ReuseViewOptF[F, B] =
    zoomOpt(prism.getOption _)(prism.modify)

  def zoom[B](traversal: Traversal[A, B]): ReuseViewListF[F, B] =
    zoomList(traversal.getAll _)(traversal.modify)

  def withOnMod(f: A => F[Unit]): ReuseViewF[F, A] = map(_.withOnMod(f))

  def widen[B >: A]: ReuseViewF[F, B] = map(_.widen[B])

  def unsafeNarrow[B <: A]: ReuseViewF[F, B] =
    zoom(_.asInstanceOf[B])(modB => a => modB(a.asInstanceOf[B]))

  def to[F1[_]: Monad](
    toF1:   F[Unit] => F1[Unit],
    fromF1: F1[Unit] => F[Unit]
  ): ReuseViewF[F1, A] = ReuseViewF[F1, A](rv.map(_.to[F1](toF1, fromF1)))

  def mapValue[B, C](f: ViewF[F, B] => C)(implicit ev: A =:= Option[B]): Option[C] =
    get.map(a => f(rv.zoom(_ => a)(f => a1 => ev.flip(a1.map(f)))))

  override def toString(): String = s"ReuseViewF($get, <modFn>)"
}

object ReuseViewF {
  def apply[F[_]: Monad, A](rv: Reuse[ViewF[F, A]]): ReuseViewF[F, A] = new ReuseViewF(rv)

  implicit def reuseReuseViewF[F[_], A]: Reusability[ReuseViewF[F, A]] = Reusability.by(_.rv)
}

class ReuseViewOptF[F[_]: Monad, A](val rvo: Reuse[ViewOptF[F, A]]) extends ViewOps[F, Option, A] {
  val get: Option[A]                                           = rvo.value.get
  val modCB: ((A => A), Option[A] => F[Unit]) => F[Unit]       = rvo.value.modCB
  def modAndGet(f: A => A)(implicit F: Async[F]): F[Option[A]] = rvo.value.modAndGet(f)

  def map[B](f: ViewOptF[F, A] => ViewOptF[F, B]): ReuseViewOptF[F, B] =
    ReuseViewOptF[F, B](rvo.map(f))

  def as[B](iso: Iso[A, B]): ReuseViewOptF[F, B] = zoom(iso.asLens)

  def asList: ReuseViewListF[F, A] = zoom(Iso.id[A].asTraversal)

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ReuseViewOptF[F, B] =
    map(_.zoom(getB)(modB))

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ReuseViewOptF[F, B] =
    map(_.zoomOpt(getB)(modB))

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ReuseViewListF[F, B] =
    ReuseViewListF(rvo.map(_.zoomList(getB)(modB)))

  def zoom[B](lens: Lens[A, B]): ReuseViewOptF[F, B] =
    zoom(lens.get _)(lens.modify)

  def zoom[B](optional: Optional[A, B]): ReuseViewOptF[F, B] =
    zoomOpt(optional.getOption)(optional.modify)

  def zoom[B](prism: Prism[A, B]): ReuseViewOptF[F, B] =
    zoomOpt(prism.getOption)(prism.modify)

  def zoom[B](
    traversal: Traversal[A, B]
  ): ReuseViewListF[F, B] =
    zoomList(traversal.getAll)(traversal.modify)

  def withOnMod(f: Option[A] => F[Unit]): ReuseViewOptF[F, A] = map(_.withOnMod(f))

  def widen[B >: A]: ReuseViewOptF[F, B] = map(_.widen[B])

  def unsafeNarrow[B <: A]: ReuseViewOptF[F, B] = map(_.unsafeNarrow[B])

  def mapValue[B](f: ViewF[F, A] => B)(implicit ev: Monoid[F[Unit]]): Option[B] =
    get.map(a => f(ViewF[F, A](a, (mod, cb) => modCB(mod, _.foldMap(cb)))))

  override def toString(): String = s"ReuseViewOptF($get, <modFn>)"
}

object ReuseViewOptF {
  def apply[F[_]: Monad, A](rvo: Reuse[ViewOptF[F, A]]): ReuseViewOptF[F, A] =
    new ReuseViewOptF(rvo)

  implicit def reuseReuseViewOptF[F[_], A]: Reusability[ReuseViewOptF[F, A]] =
    Reusability.by(_.rvo)
}

class ReuseViewListF[F[_]: Monad, A](val rvl: Reuse[ViewListF[F, A]]) extends ViewOps[F, List, A] {
  val get: List[A]                                           = rvl.value.get
  val modCB: ((A => A), List[A] => F[Unit]) => F[Unit]       = rvl.value.modCB
  def modAndGet(f: A => A)(implicit F: Async[F]): F[List[A]] = rvl.value.modAndGet(f)

  def map[B](f: ViewListF[F, A] => ViewListF[F, B]): ReuseViewListF[F, B] =
    ReuseViewListF[F, B](rvl.map(f))

  def as[B](iso: Iso[A, B]): ReuseViewListF[F, B] = zoom(iso.asLens)

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ReuseViewListF[F, B] =
    map(_.zoom(getB)(modB))

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ReuseViewListF[F, B] =
    map(_.zoomOpt(getB)(modB))

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ReuseViewListF[F, B] =
    map(_.zoomList(getB)(modB))

  def zoom[B](lens: Lens[A, B]): ReuseViewListF[F, B] =
    zoom(lens.get _)(lens.modify)

  def zoom[B](optional: Optional[A, B]): ReuseViewListF[F, B] =
    zoomOpt(optional.getOption)(optional.modify)

  def zoom[B](prism: Prism[A, B]): ReuseViewListF[F, B] =
    zoomOpt(prism.getOption)(prism.modify)

  def zoom[B](
    traversal: Traversal[A, B]
  ): ReuseViewListF[F, B] =
    zoomList(traversal.getAll)(traversal.modify)

  def withOnMod(f: List[A] => F[Unit]): ReuseViewListF[F, A] = map(_.withOnMod(f))

  def widen[B >: A]: ReuseViewListF[F, B] = map(_.widen[B])

  override def toString(): String = s"ReuseViewListF($get, <modFn>)"
}

object ReuseViewListF {
  def apply[F[_]: Monad, A](rvl: Reuse[ViewListF[F, A]]): ReuseViewListF[F, A] =
    new ReuseViewListF(rvl)

  implicit def reuseReuseViewListF[F[_], A]: Reusability[ReuseViewListF[F, A]] =
    Reusability.by(_.rvl)
}
