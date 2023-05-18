// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Id
import cats.Monad
import cats.Monoid
import cats.effect._
import cats.syntax.all._
import monocle.Iso
import monocle.Lens
import monocle.Optional
import monocle.Prism
import monocle.Traversal

sealed abstract class ViewOps[F[_]: Monad, G[_], A] {
  val get: G[A]

  val setCB: (A, G[A] => F[Unit]) => F[Unit] = (a, cb) => modCB(_ => a, cb)

  val set: A => F[Unit] = a => mod(_ => a)

  val modCB: (A => A, G[A] => F[Unit]) => F[Unit]

  val mod: (A => A) => F[Unit] = f => modCB(f, _ => Monad[F].unit)

  def modAndGet(f: A => A)(implicit F: Async[F]): F[G[A]]
}

// The difference between a View and a StateSnapshot is that the modifier doesn't act on the current value,
// but passes the modifier function to an external source of truth. Since we are defining no getter
// from such source of truth, a View is defined in terms of a modifier function instead of a setter.
class ViewF[F[_]: Monad, A](val get: A, val modCB: (A => A, A => F[Unit]) => F[Unit])
    extends ViewOps[F, Id, A] { self =>
  def modAndExtract[B](f: (A => (A, B)))(implicit F: Async[F]): F[B] =
    Async[F].async { cb =>
      mod { (a: A) =>
        val (fa, b) = f(a)
        cb(b.asRight)
        fa
      // No need to run cb on errors, it will fail the async installation effect.
      }.as(none)
    }

  // In a ViewF, we can derive modAndGet. In ViewOptF and ViewListF we have to pass it, since their
  // mod functions have no idea of the enclosing structure.
  def modAndGet(f: A => A)(implicit F: Async[F]): F[A] =
    modAndExtract(f.andThen(a => (a, a)))

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ViewF[F, B] =
    new ViewF(
      getB(get),
      (f: B => B, cb: B => F[Unit]) => modCB(modB(f), cb.compose(getB))
    )

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(getB(get),
                 (f: B => B, cb: Option[B] => F[Unit]) => modCB(modB(f), cb.compose(getB))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[Option[B]] =
        self.modAndGet(modB(f)).map(getB)
    }

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(getB(get),
                  (f: B => B, cb: List[B] => F[Unit]) => modCB(modB(f), cb.compose(getB))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[List[B]] =
        self.modAndGet(modB(f)).map(getB)
    }

  def as[B](iso: Iso[A, B]): ViewF[F, B] = zoom(iso.asLens)

  def asViewOpt: ViewOptF[F, A] = zoom(Iso.id[A].asOptional)

  def asViewList: ViewListF[F, A] = zoom(Iso.id[A].asTraversal)

  def zoom[B](lens: Lens[A, B]): ViewF[F, B] =
    zoom(lens.get _)(lens.modify)

  def zoom[B](optional: Optional[A, B]): ViewOptF[F, B] =
    zoomOpt(optional.getOption _)(optional.modify)

  def zoom[B](prism: Prism[A, B]): ViewOptF[F, B] =
    zoomOpt(prism.getOption _)(prism.modify)

  def zoom[B](traversal: Traversal[A, B]): ViewListF[F, B] =
    zoomList(traversal.getAll _)(traversal.modify)

  def withOnMod(f: A => F[Unit]): ViewF[F, A] =
    new ViewF[F, A](
      get,
      (modF, cb) => modCB(modF, a => f(a) >> cb(a))
    )

  def widen[B >: A]: ViewF[F, B] = self.asInstanceOf[ViewF[F, B]]

  def unsafeNarrow[B <: A]: ViewF[F, B] =
    zoom(_.asInstanceOf[B])(modB => a => modB(a.asInstanceOf[B]))

  def to[F1[_]: Monad](toF1: F[Unit] => F1[Unit], fromF1: F1[Unit] => F[Unit]): ViewF[F1, A] =
    ViewF(get, (mod, cb) => toF1(modCB(mod, a => fromF1(cb(a)))))

  def mapValue[B, C](f: ViewF[F, B] => C)(implicit ev: A =:= Option[B]): Option[C] =
    // _.get is safe here since it's only being called when the value is defined.
    // The zoom getter function is stored to use in callbacks, se we have to pass _.get
    // instead of capturing the value here. Otherwise, callbacks see a stale value.
    get.map(_ => f(zoom(_.get)(f => a1 => ev.flip(a1.map(f)))))

  def when(cond: A => Boolean): Boolean = cond(get)

  def unless(cond: A => Boolean): Boolean = !when(cond)

  override def toString(): String = s"ViewF($get, <modFn>)"
}

object ViewF {
  def apply[F[_]: Monad, A](value: A, modCB: (A => A, A => F[Unit]) => F[Unit]): ViewF[F, A] =
    new ViewF(value, modCB)
}

abstract class ViewOptF[F[_]: Monad, A](
  val get:   Option[A],
  val modCB: (A => A, Option[A] => F[Unit]) => F[Unit]
) extends ViewOps[F, Option, A] { self =>
  def as[B](iso: Iso[A, B]): ViewOptF[F, B] = zoom(iso.asLens)

  def exists(cond: A => Boolean): Boolean = get.exists(cond)

  def forall(cond: A => Boolean): Boolean = get.forall(cond)

  def contains(cond: A): Boolean = get.contains(cond)

  def asViewList: ViewListF[F, A] = zoom(Iso.id[A].asTraversal)

  def asView(implicit ev: Monoid[F[Unit]]): Option[ViewF[F, A]] = mapValue(identity)

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(
      get.map(getB),
      (f: B => B, cb: Option[B] => F[Unit]) => modCB(modB(f), cb.compose(_.map(getB)))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[Option[B]] =
        self.modAndGet(modB(f)).map(_.map(getB))
    }

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(get.flatMap(getB),
                 (f: B => B, cb: Option[B] => F[Unit]) =>
                   modCB(modB(f), cb.compose(_.flatMap(getB)))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[Option[B]] =
        self.modAndGet(modB(f)).map(_.flatMap(getB))
    }

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(get.map(getB).orEmpty,
                  (f: B => B, cb: List[B] => F[Unit]) =>
                    modCB(modB(f), cb.compose(_.toList.flatMap(getB)))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[List[B]] =
        self.modAndGet(modB(f)).map(_.map(getB).orEmpty)
    }

  def zoom[B](lens: Lens[A, B]): ViewOptF[F, B] =
    zoom(lens.get _)(lens.modify)

  def zoom[B](optional: Optional[A, B]): ViewOptF[F, B] =
    zoomOpt(optional.getOption)(optional.modify)

  def zoom[B](prism: Prism[A, B]): ViewOptF[F, B] =
    zoomOpt(prism.getOption)(prism.modify)

  def zoom[B](
    traversal: Traversal[A, B]
  ): ViewListF[F, B] =
    zoomList(traversal.getAll)(traversal.modify)

  def withOnMod(f: Option[A] => F[Unit]): ViewOptF[F, A] =
    new ViewOptF[F, A](
      get,
      (modF, cb) => modCB(modF, a => f(a) >> cb(a))
    ) {
      def modAndGet(f: A => A)(implicit F: Async[F]): F[Option[A]] =
        self.modAndGet(f)
    }

  def widen[B >: A]: ViewOptF[F, B] = self.asInstanceOf[ViewOptF[F, B]]

  def unsafeNarrow[B <: A]: ViewOptF[F, B] =
    zoom(_.asInstanceOf[B])(modB => a => modB(a.asInstanceOf[B]))

  def mapValue[B](f: ViewF[F, A] => B)(implicit ev: Monoid[F[Unit]]): Option[B] =
    get.map(a => f(ViewF[F, A](a, (mod, cb) => modCB(mod, _.foldMap(cb)))))

  override def toString(): String = s"ViewOptF($get, <modFn>)"
}

abstract class ViewListF[F[_]: Monad, A](
  val get:   List[A],
  val modCB: (A => A, List[A] => F[Unit]) => F[Unit]
) extends ViewOps[F, List, A] { self =>
  def as[B](iso: Iso[A, B]): ViewListF[F, B] = zoom(iso.asLens)

  def exists(cond: A => Boolean): Boolean = get.exists(cond)

  def forall(cond: A => Boolean): Boolean = get.forall(cond)

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(get.map(getB),
                  (f: B => B, cb: List[B] => F[Unit]) => modCB(modB(f), cb.compose(_.map(getB)))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[List[B]] =
        self.modAndGet(modB(f)).map(_.map(getB))
    }

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(get.flatMap(getB),
                  (f: B => B, cb: List[B] => F[Unit]) => modCB(modB(f), cb.compose(_.flatMap(getB)))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[List[B]] =
        self.modAndGet(modB(f)).map(_.flatMap(getB))
    }

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(
      get.flatMap(getB),
      (f: B => B, cb: List[B] => F[Unit]) => modCB(modB(f), cb.compose(_.flatMap(getB)))
    ) {
      def modAndGet(f: B => B)(implicit F: Async[F]): F[List[B]] =
        self.modAndGet(modB(f)).map(_.flatMap(getB))
    }

  def zoom[B](lens: Lens[A, B]): ViewListF[F, B] =
    zoom(lens.get _)(lens.modify)

  def zoom[B](optional: Optional[A, B]): ViewListF[F, B] =
    zoomOpt(optional.getOption)(optional.modify)

  def zoom[B](prism: Prism[A, B]): ViewListF[F, B] =
    zoomOpt(prism.getOption)(prism.modify)

  def zoom[B](
    traversal: Traversal[A, B]
  ): ViewListF[F, B] =
    zoomList(traversal.getAll)(traversal.modify)

  def withOnMod(f: List[A] => F[Unit]): ViewListF[F, A] =
    new ViewListF[F, A](
      get,
      (modF, cb) => modCB(modF, a => f(a) >> cb(a))
    ) {
      def modAndGet(f: A => A)(implicit F: Async[F]): F[List[A]] =
        self.modAndGet(f)
    }

  def widen[B >: A]: ViewListF[F, B] = self.asInstanceOf[ViewListF[F, B]]

  override def toString(): String = s"ViewListF($get, <modFn>)"
}
