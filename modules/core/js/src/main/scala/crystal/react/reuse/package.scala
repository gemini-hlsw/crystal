// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.reuse

import cats.Monad
import cats.Monoid
import cats.effect.Async
import cats.syntax.all.*
import crystal.ViewF
import crystal.ViewListF
import crystal.ViewOptF
import japgolly.scalajs.react.Reusability
import monocle.Iso
import monocle.Lens
import monocle.Optional
import monocle.Prism
import monocle.Traversal

import scala.annotation.targetName
import scala.reflect.ClassTag

type ==>[A, B] = Reuse[A => B]

extension [A](a: A)
  def reuseAlways: Reuse[A] = Reuse.always(a)

  def reuseNever: Reuse[A] = Reuse.never(a)

  /*
   * Implements the idiom:
   *   `a.curryReusing( (A, B) => C )`
   * to create a `Reusable[B => C]` with `Reusability[A]`.
   *
   * Works for other arities too, as implemented in `Reuse.Curried1`.
   */
  def curryReusing: Reuse.Curried1[A] = Reuse.currying(a)

extension [R, S](t: (R, S))
  /*
   * Implements the idiom:
   *   `(a, b).curryReusing( (A, B, C) => D )`
   * to create a `Reusable[C => D]` with `Reusability[(A, B)]`.
   *
   * Works for other arities too, as implemented in `Reuse.Curried2`.
   */
  def curryReusing: Reuse.Curried2[R, S] = Reuse.currying(t._1, t._2)

extension [R, S, T](t: (R, S, T))
  /*
   * Implements the idiom:
   *   `(a, b, c).curryReusing( (A, B, C, D) => E )`
   * to create a `Reusable[D => E]` with `Reusability[(A, B, C)]`.
   *
   * Works for other arities too, as implemented in `Reuse.Curried3`.
   */
  def curryReusing: Reuse.Curried3[R, S, T] = Reuse.currying(t._1, t._2, t._3)

extension [R, S, T, U](t: (R, S, T, U))
  /*
   * Implements the idiom:
   *   `(a, b, c, d).curryReusing( (A, B, C, D, E) => F )`
   * to create a `Reusable[E => F]` with `Reusability[(A, B, C, D)]`.
   *
   * Works for other arities too, as implemented in `Reuse.Curried4`.
   */
  def curryReusing: Reuse.Curried4[R, S, T, U] = Reuse.currying(t._1, t._2, t._3, t._4)

extension [R, S, T, U, V](t: (R, S, T, U, V))
  /*
   * Implements the idiom:
   *   `(a, b, c, d, e).curryReusing( (A, B, C, D, E, F) => G )`
   * to create a `Reusable[F => G]` with `Reusability[(A, B, C, D, E)]`.
   *
   * Works for other arities too, as implemented in `Reuse.Curried5`.
   */
  def curryReusing: Reuse.Curried5[R, S, T, U, V] = Reuse.currying(t._1, t._2, t._3, t._4, t._5)

extension [R, B](fn: R => B)
  /*
   * Implements the idiom:
   *   `(R => B).reuseCurrying(r)`
   * to create a `Reusable[B]` with `Reusability[R]`.
   */
  def reuseCurrying(
    r:         R
  )(using
    classTagR: ClassTag[R],
    reuseR:    Reusability[R]
  ): Reuse[B] = Reuse.currying(r).in(fn)

extension [R, S, B](fn: (R, S) => B)
  /*
   * Implements the idiom:
   *   `((R, S) => B).reuseCurrying(r)`
   * to create a `Reusable[S => B]` with `Reusability[R]`.
   */
  def reuseCurrying(
    r:         R
  )(using
    classTagR: ClassTag[R],
    reuseR:    Reusability[R]
  ): Reuse[S => B] = Reuse.currying(r).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S) => B).reuseCurrying(r, s)`
   * to create a `Reusable[B]` with `Reusability[(R, S)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S
  )(using
    classTagR: ClassTag[(R, S)],
    reuseR:    Reusability[(R, S)]
  ): Reuse[B] = Reuse.currying(r, s).in(fn)

extension [R, S, T, B](fn: (R, S, T) => B)
  /*
   * Implements the idiom:
   *   `((R, S, T) => B).reuseCurrying(r)`
   * to create a `Reusable[(S, T) => B]` with `Reusability[R]`.
   */
  def reuseCurrying(
    r:         R
  )(using
    classTagR: ClassTag[R],
    reuseR:    Reusability[R]
  ): Reuse[(S, T) => B] = Reuse.currying(r).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T) => B).reuseCurrying(r, s)`
   * to create a `Reusable[T => B]` with `Reusability[(R, S)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S
  )(using
    classTagR: ClassTag[(R, S)],
    reuseR:    Reusability[(R, S)]
  ): Reuse[T => B] = Reuse.currying(r, s).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T) => B).reuseCurrying(r, s, t)`
   * to create a `Reusable[B]` with `Reusability[(R, S, T)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S,
    t:         T
  )(using
    classTagR: ClassTag[(R, S, T)],
    reuseR:    Reusability[(R, S, T)]
  ): Reuse[B] = Reuse.currying(r, s, t).in(fn)

extension [R, S, T, U, B](fn: (R, S, T, U) => B)
  /*
   * Implements the idiom:
   *   `((R, S, T, U) => B).reuseCurrying(r)`
   * to create a `Reusable[(S, T, U) => B]` with `Reusability[R]`.
   */
  def reuseCurrying(
    r:         R
  )(using
    classTagR: ClassTag[R],
    reuseR:    Reusability[R]
  ): Reuse[(S, T, U) => B] = Reuse.currying(r).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T, U) => B).reuseCurrying(r, s)`
   * to create a `Reusable[(T, U) => B]` with `Reusability[(R, S)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S
  )(using
    classTagR: ClassTag[(R, S)],
    reuseR:    Reusability[(R, S)]
  ): Reuse[(T, U) => B] = Reuse.currying(r, s).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T, U) => B).reuseCurrying(r, s, t)`
   * to create a `Reusable[U => B]` with `Reusability[(R, S, T)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S,
    t:         T
  )(using
    classTagR: ClassTag[(R, S, T)],
    reuseR:    Reusability[(R, S, T)]
  ): Reuse[U => B] = Reuse.currying(r, s, t).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T, U) => B).reuseCurrying(r, s, t, u)`
   * to create a `Reusable[B]` with `Reusability[(R, S, T, U)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S,
    t:         T,
    u:         U
  )(using
    classTagR: ClassTag[(R, S, T, U)],
    reuseR:    Reusability[(R, S, T, U)]
  ): Reuse[B] = Reuse.currying(r, s, t, u).in(fn)

extension [R, S, T, U, V, B](fn: (R, S, T, U, V) => B)
  /*
   * Implements the idiom:
   *   `((R, S, T, U, V) => B).reuseCurrying(r)`
   * to create a `Reusable[(S, T, U, V) => B]` with `Reusability[R]`.
   */
  def reuseCurrying(
    r:         R
  )(using
    classTagR: ClassTag[R],
    reuseR:    Reusability[R]
  ): Reuse[(S, T, U, V) => B] = Reuse.currying(r).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T, U, V) => B).reuseCurrying(r, s)`
   * to create a `Reusable[(T, U, V) => B]` with `Reusability[(R, S)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S
  )(using
    classTagR: ClassTag[(R, S)],
    reuseR:    Reusability[(R, S)]
  ): Reuse[(T, U, V) => B] = Reuse.currying(r, s).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T, U, V) => B).reuseCurrying(r, s, t)`
   * to create a `Reusable[(U, V) => B]` with `Reusability[(R, S, T)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S,
    t:         T
  )(using
    classTagR: ClassTag[(R, S, T)],
    reuseR:    Reusability[(R, S, T)]
  ): Reuse[(U, V) => B] = Reuse.currying(r, s, t).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T, U, V) => B).reuseCurrying(r, s, t, u)`
   * to create a `Reusable[V => B]` with `Reusability[(R, S, T, U)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S,
    t:         T,
    u:         U
  )(using
    classTagR: ClassTag[(R, S, T, U)],
    reuseR:    Reusability[(R, S, T, U)]
  ): Reuse[V => B] = Reuse.currying(r, s, t, u).in(fn)

  /*
   * Implements the idiom:
   *   `((R, S, T, U, V) => B).reuseCurrying(r, s, t, u, v)`
   * to create a `Reusable[B]` with `Reusability[(R, S, T, U, V)]`.
   */
  def reuseCurrying(
    r:         R,
    s:         S,
    t:         T,
    u:         U,
    v:         V
  )(using
    classTagR: ClassTag[(R, S, T, U, V)],
    reuseR:    Reusability[(R, S, T, U, V)]
  ): Reuse[B] = Reuse.currying(r, s, t, u, v).in(fn)

extension [F[_]: Monad, A](rv: Reuse[ViewF[F, A]])
  def get: A = rv.value.get

  def modCB: (A => A, A => F[Unit]) => F[Unit] = rv.value.modCB

  def modAndGet(f: A => A)(using F: Async[F]): F[A] = rv.value.modAndGet(f)

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): Reuse[ViewF[F, B]] =
    rv.map(_.zoom(getB)(modB))

  def zoomOpt[B](getB: A => Option[B])(modB: (B => B) => A => A): Reuse[ViewOptF[F, B]] =
    rv.map(_.zoomOpt(getB)(modB))

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] = rv.map(_.zoomList(getB)(modB))

  def as[B](iso: Iso[A, B]): Reuse[ViewF[F, B]] = zoom(iso.asLens)

  def asViewOpt: Reuse[ViewOptF[F, A]] = zoom(Iso.id[A].asOptional)

  def asList: Reuse[ViewListF[F, A]] = zoom(Iso.id[A].asTraversal)

  def zoom[B](lens: Lens[A, B]): Reuse[ViewF[F, B]] = zoom(lens.get)(lens.modify)

  def zoom[B](optional: Optional[A, B]): Reuse[ViewOptF[F, B]] =
    zoomOpt(optional.getOption)(optional.modify)

  def zoom[B](prism: Prism[A, B]): Reuse[ViewOptF[F, B]] =
    zoomOpt(prism.getOption)(prism.modify)

  def zoom[B](traversal: Traversal[A, B]): Reuse[ViewListF[F, B]] =
    zoomList(traversal.getAll)(traversal.modify)

  def withOnMod(f: A => F[Unit]): Reuse[ViewF[F, A]] = rv.map(_.withOnMod(f))

  def widen[B >: A]: Reuse[ViewF[F, B]] = rv.map(_.widen[B])

  def unsafeNarrow[B <: A]: Reuse[ViewF[F, B]] =
    zoom(_.asInstanceOf[B])(modB => a => modB(a.asInstanceOf[B]))

  def to[F1[_]: Monad](
    toF1:   F[Unit] => F1[Unit],
    fromF1: F1[Unit] => F[Unit]
  ): Reuse[ViewF[F1, A]] = rv.map(_.to[F1](toF1, fromF1))

  def mapValue[B, C](f: Reuse[ViewF[F, B]] => C)(using ev: A =:= Option[B]): Option[C] =
    get.map(a => f(zoom(_ => a)(f => a1 => ev.flip(a1.map(f)))))

extension [F[_]: Monad, A](rvo: Reuse[ViewOptF[F, A]])
  def get: Option[A] = rvo.value.get

  @targetName("reuseViewOptModCB")
  def modCB: (A => A, Option[A] => F[Unit]) => F[Unit] = rvo.value.modCB

  @targetName("reuseViewOptModAndGet")
  def modAndGet(f: A => A)(using F: Async[F]): F[Option[A]] = rvo.value.modAndGet(f)

  @targetName("reuseViewOptAs")
  def as[B](iso: Iso[A, B]): Reuse[ViewOptF[F, B]] = zoom(iso.asLens)

  @targetName("reuseViewOptAsList")
  def asList: Reuse[ViewListF[F, A]] = zoom(Iso.id[A].asTraversal)

  @targetName("reuseViewOptZoom")
  def zoom[B](getB: A => B)(modB: (B => B) => A => A): Reuse[ViewOptF[F, B]] =
    rvo.map(_.zoom(getB)(modB))

  @targetName("reuseViewOptZoomOpt")
  def zoomOpt[B](getB: A => Option[B])(modB: (B => B) => A => A): Reuse[ViewOptF[F, B]] =
    rvo.map(_.zoomOpt(getB)(modB))

  @targetName("reuseViewOptZoomList")
  def zoomList[B](getB: A => List[B])(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
    rvo.map(_.zoomList(getB)(modB))

  @targetName("reuseViewOptZoomLens")
  def zoom[B](lens: Lens[A, B]): Reuse[ViewOptF[F, B]] = zoom(lens.get)(lens.modify)

  @targetName("reuseViewOptZoomOptional")
  def zoom[B](optional: Optional[A, B]): Reuse[ViewOptF[F, B]] =
    zoomOpt(optional.getOption)(optional.modify)

  @targetName("reuseViewOptZoomPrism")
  def zoom[B](prism: Prism[A, B]): Reuse[ViewOptF[F, B]] = zoomOpt(prism.getOption)(prism.modify)

  @targetName("reuseViewOptZoomTraversal")
  def zoom[B](traversal: Traversal[A, B]): Reuse[ViewListF[F, B]] =
    zoomList(traversal.getAll)(traversal.modify)

  @targetName("reuseViewOptWithOnMod")
  def withOnMod(f: Option[A] => F[Unit]): Reuse[ViewOptF[F, A]] = rvo.map(_.withOnMod(f))

  @targetName("reuseViewOptWiden")
  def widen[B >: A]: Reuse[ViewOptF[F, B]] = rvo.map(_.widen[B])

  @targetName("reuseViewOptUnsafeNarrow")
  def unsafeNarrow[B <: A]: Reuse[ViewOptF[F, B]] = rvo.map(_.unsafeNarrow[B])

  // def modCB2: (A => A, Option[A] => F[Unit]) => F[Unit] = rvo.value.modCB

  @targetName("reuseViewOptMapValue")
  def mapValue[B](f: Reuse[ViewF[F, A]] => B)(using ev: Monoid[F[Unit]]): Option[B] =
    get.map(a =>
      f(
        rvo.map(vo =>
          ViewF[F, A](
            a,
            (mod, cb) =>
              vo.modCB(
                mod,
                (previous, current) => (previous, current).tupled.foldMap((p, c) => cb(p, c))
              )
          )
        )
      )
    )

extension [F[_]: Monad, A](rvl: Reuse[ViewListF[F, A]])
  def get: List[A] = rvl.value.get

  @targetName("reuseViewListModCB")
  def modCB: (A => A, List[A] => F[Unit]) => F[Unit] = rvl.value.modCB

  @targetName("reuseViewListModAndGet")
  def modAndGet(f: A => A)(using F: Async[F]): F[List[A]] = rvl.value.modAndGet(f)

  @targetName("reuseViewListAs")
  def as[B](iso: Iso[A, B]): Reuse[ViewListF[F, B]] = zoom(iso.asLens)

  @targetName("reuseViewListZoom")
  def zoom[B](getB: A => B)(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
    rvl.map(_.zoom(getB)(modB))

  @targetName("reuseViewListZoomOpt")
  def zoomOpt[B](getB: A => Option[B])(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
    rvl.map(_.zoomOpt(getB)(modB))

  @targetName("reuseViewListZoomList")
  def zoomList[B](getB: A => List[B])(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
    rvl.map(_.zoomList(getB)(modB))

  @targetName("reuseViewListZoomLens")
  def zoom[B](lens: Lens[A, B]): Reuse[ViewListF[F, B]] = zoom(lens.get)(lens.modify)

  @targetName("reuseViewListZoomOptional")
  def zoom[B](optional: Optional[A, B]): Reuse[ViewListF[F, B]] =
    zoomOpt(optional.getOption)(optional.modify)

  @targetName("reuseViewListZoomPrism")
  def zoom[B](prism: Prism[A, B]): Reuse[ViewListF[F, B]] = zoomOpt(prism.getOption)(prism.modify)

  @targetName("reuseViewListZoomTraversal")
  def zoom[B](traversal: Traversal[A, B]): Reuse[ViewListF[F, B]] =
    zoomList(traversal.getAll)(traversal.modify)

  @targetName("reuseViewListWithOnMod")
  def withOnMod(f: List[A] => F[Unit]): Reuse[ViewListF[F, A]] = rvl.map(_.withOnMod(f))

  @targetName("reuseViewListWiden")
  def widen[B >: A]: Reuse[ViewListF[F, B]] = rvl.map(_.widen[B])
