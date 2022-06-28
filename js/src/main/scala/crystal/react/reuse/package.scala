package crystal.react

import cats.Monad
import cats.Monoid
import cats.effect.Async
import cats.syntax.all._
import crystal.ViewF
import crystal.ViewListF
import crystal.ViewOptF
import crystal.react.reuse.Reuse
import japgolly.scalajs.react.Reusability
import monocle.Iso
import monocle.Lens
import monocle.Optional
import monocle.Prism
import monocle.Traversal

import scala.reflect.ClassTag

trait ReuseImplicitsLowPriority {
  implicit def toA[A](reuseFn: Reuse[A]): A = reuseFn.value
}
package object reuse extends ReuseImplicitsLowPriority {
  type ==>[A, B] = Reuse[A => B]

  implicit class AnyReuseOps[A](private val a: A) extends AnyVal {
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
  }

  implicit class Tuple2ReuseOps[R, S](private val t: (R, S)) extends AnyVal {
    /*
     * Implements the idiom:
     *   `(a, b).curryReusing( (A, B, C) => D )`
     * to create a `Reusable[C => D]` with `Reusability[(A, B)]`.
     *
     * Works for other arities too, as implemented in `Reuse.Curried2`.
     */
    def curryReusing: Reuse.Curried2[R, S] = Reuse.currying(t._1, t._2)
  }

  implicit class Tuple3ReuseOps[R, S, T](private val t: (R, S, T)) extends AnyVal {
    /*
     * Implements the idiom:
     *   `(a, b, c).curryReusing( (A, B, C, D) => E )`
     * to create a `Reusable[D => E]` with `Reusability[(A, B, C)]`.
     *
     * Works for other arities too, as implemented in `Reuse.Curried3`.
     */
    def curryReusing: Reuse.Curried3[R, S, T] = Reuse.currying(t._1, t._2, t._3)
  }

  implicit class Tuple4ReuseOps[R, S, T, U](private val t: (R, S, T, U)) extends AnyVal {
    /*
     * Implements the idiom:
     *   `(a, b, c, d).curryReusing( (A, B, C, D, E) => F )`
     * to create a `Reusable[E => F]` with `Reusability[(A, B, C, D)]`.
     *
     * Works for other arities too, as implemented in `Reuse.Curried4`.
     */
    def curryReusing: Reuse.Curried4[R, S, T, U] = Reuse.currying(t._1, t._2, t._3, t._4)
  }

  implicit class Tuple5ReuseOps[R, S, T, U, V](private val t: (R, S, T, U, V)) extends AnyVal {
    /*
     * Implements the idiom:
     *   `(a, b, c, d, e).curryReusing( (A, B, C, D, E, F) => G )`
     * to create a `Reusable[F => G]` with `Reusability[(A, B, C, D, E)]`.
     *
     * Works for other arities too, as implemented in `Reuse.Curried5`.
     */
    def curryReusing: Reuse.Curried5[R, S, T, U, V] = Reuse.currying(t._1, t._2, t._3, t._4, t._5)
  }

  implicit class Fn1ReuseOps[R, B](private val fn: R => B) extends AnyVal {
    /*
     * Implements the idiom:
     *   `(R => B).reuseCurrying(r)`
     * to create a `Reusable[B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
      classTagR: ClassTag[R],
      reuseR:    Reusability[R]
    ): Reuse[B] = Reuse.currying(r).in(fn)
  }

  implicit class Fn2ReuseOps[R, S, B](private val fn: (R, S) => B) extends AnyVal {
    /*
     * Implements the idiom:
     *   `((R, S) => B).reuseCurrying(r)`
     * to create a `Reusable[S => B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
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
    )(implicit
      classTagR: ClassTag[(R, S)],
      reuseR:    Reusability[(R, S)]
    ): Reuse[B] = Reuse.currying(r, s).in(fn)
  }

  implicit class Fn3ReuseOps[R, S, T, B](private val fn: (R, S, T) => B) extends AnyVal {
    /*
     * Implements the idiom:
     *   `((R, S, T) => B).reuseCurrying(r)`
     * to create a `Reusable[(S, T) => B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
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
    )(implicit
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
    )(implicit
      classTagR: ClassTag[(R, S, T)],
      reuseR:    Reusability[(R, S, T)]
    ): Reuse[B] = Reuse.currying(r, s, t).in(fn)
  }

  implicit class Fn4ReuseOps[R, S, T, U, B](private val fn: (R, S, T, U) => B) extends AnyVal {
    /*
     * Implements the idiom:
     *   `((R, S, T, U) => B).reuseCurrying(r)`
     * to create a `Reusable[(S, T, U) => B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
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
    )(implicit
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
    )(implicit
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
    )(implicit
      classTagR: ClassTag[(R, S, T, U)],
      reuseR:    Reusability[(R, S, T, U)]
    ): Reuse[B] = Reuse.currying(r, s, t, u).in(fn)
  }

  implicit class Fn5ReuseOps[R, S, T, U, V, B](private val fn: (R, S, T, U, V) => B)
      extends AnyVal {
    /*
     * Implements the idiom:
     *   `((R, S, T, U, V) => B).reuseCurrying(r)`
     * to create a `Reusable[(S, T, U, V) => B]` with `Reusability[R]`.
     */
    def reuseCurrying(
      r:         R
    )(implicit
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
    )(implicit
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
    )(implicit
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
    )(implicit
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
    )(implicit
      classTagR: ClassTag[(R, S, T, U, V)],
      reuseR:    Reusability[(R, S, T, U, V)]
    ): Reuse[B] = Reuse.currying(r, s, t, u, v).in(fn)
  }

  implicit class ReuseViewFOps[F[_]: Monad, A](val rv: Reuse[ViewF[F, A]]) {
    val get: A                                           = rv.value.get
    val modCB: ((A => A), A => F[Unit]) => F[Unit]       = rv.value.modCB
    def modAndGet(f: A => A)(implicit F: Async[F]): F[A] = rv.value.modAndGet(f)

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

    def zoom[B](lens: Lens[A, B]): Reuse[ViewF[F, B]] = zoom(lens.get _)(lens.modify)

    def zoom[B](optional: Optional[A, B]): Reuse[ViewOptF[F, B]] =
      zoomOpt(optional.getOption _)(optional.modify)

    def zoom[B](prism: Prism[A, B]): Reuse[ViewOptF[F, B]] =
      zoomOpt(prism.getOption _)(prism.modify)

    def zoom[B](traversal: Traversal[A, B]): Reuse[ViewListF[F, B]] =
      zoomList(traversal.getAll _)(traversal.modify)

    def withOnMod(f: A => F[Unit]): Reuse[ViewF[F, A]] = rv.map(_.withOnMod(f))

    def widen[B >: A]: Reuse[ViewF[F, B]] = rv.map(_.widen[B])

    def unsafeNarrow[B <: A]: Reuse[ViewF[F, B]] =
      zoom(_.asInstanceOf[B])(modB => a => modB(a.asInstanceOf[B]))

    def to[F1[_]: Monad](
      toF1:   F[Unit] => F1[Unit],
      fromF1: F1[Unit] => F[Unit]
    ): Reuse[ViewF[F1, A]] = rv.map(_.to[F1](toF1, fromF1))

    def mapValue[B, C](f: Reuse[ViewF[F, B]] => C)(implicit ev: A =:= Option[B]): Option[C] =
      get.map(a => f(zoom(_ => a)(f => a1 => ev.flip(a1.map(f)))))
  }

  implicit class ReuseViewOptFOps[F[_]: Monad, A](val rvo: Reuse[ViewOptF[F, A]]) {
    val get: Option[A]                                           = rvo.value.get
    val modCB: ((A => A), Option[A] => F[Unit]) => F[Unit]       = rvo.value.modCB
    def modAndGet(f: A => A)(implicit F: Async[F]): F[Option[A]] = rvo.value.modAndGet(f)

    def as[B](iso: Iso[A, B]): Reuse[ViewOptF[F, B]] = zoom(iso.asLens)

    def asList: Reuse[ViewListF[F, A]] = zoom(Iso.id[A].asTraversal)

    def zoom[B](getB: A => B)(modB: (B => B) => A => A): Reuse[ViewOptF[F, B]] =
      rvo.map(_.zoom(getB)(modB))

    def zoomOpt[B](getB: A => Option[B])(modB: (B => B) => A => A): Reuse[ViewOptF[F, B]] =
      rvo.map(_.zoomOpt(getB)(modB))

    def zoomList[B](getB: A => List[B])(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
      rvo.map(_.zoomList(getB)(modB))

    def zoom[B](lens: Lens[A, B]): Reuse[ViewOptF[F, B]] = zoom(lens.get _)(lens.modify)

    def zoom[B](optional: Optional[A, B]): Reuse[ViewOptF[F, B]] =
      zoomOpt(optional.getOption)(optional.modify)

    def zoom[B](prism: Prism[A, B]): Reuse[ViewOptF[F, B]] = zoomOpt(prism.getOption)(prism.modify)

    def zoom[B](traversal: Traversal[A, B]): Reuse[ViewListF[F, B]] =
      zoomList(traversal.getAll)(traversal.modify)

    def withOnMod(f: Option[A] => F[Unit]): Reuse[ViewOptF[F, A]] = rvo.map(_.withOnMod(f))

    def widen[B >: A]: Reuse[ViewOptF[F, B]] = rvo.map(_.widen[B])

    def unsafeNarrow[B <: A]: Reuse[ViewOptF[F, B]] = rvo.map(_.unsafeNarrow[B])

    def mapValue[B](f: Reuse[ViewF[F, A]] => B)(implicit ev: Monoid[F[Unit]]): Option[B] =
      get.map(a => f(rvo.map(_ => ViewF[F, A](a, (mod, cb) => modCB(mod, _.foldMap(cb))))))
  }

  implicit class ReuseViewListFOps[F[_]: Monad, A](val rvl: Reuse[ViewListF[F, A]]) {
    val get: List[A]                                           = rvl.value.get
    val modCB: ((A => A), List[A] => F[Unit]) => F[Unit]       = rvl.value.modCB
    def modAndGet(f: A => A)(implicit F: Async[F]): F[List[A]] = rvl.value.modAndGet(f)

    def as[B](iso: Iso[A, B]): Reuse[ViewListF[F, B]] = zoom(iso.asLens)

    def zoom[B](getB: A => B)(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
      rvl.map(_.zoom(getB)(modB))

    def zoomOpt[B](getB: A => Option[B])(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
      rvl.map(_.zoomOpt(getB)(modB))

    def zoomList[B](getB: A => List[B])(modB: (B => B) => A => A): Reuse[ViewListF[F, B]] =
      rvl.map(_.zoomList(getB)(modB))

    def zoom[B](lens: Lens[A, B]): Reuse[ViewListF[F, B]] = zoom(lens.get _)(lens.modify)

    def zoom[B](optional: Optional[A, B]): Reuse[ViewListF[F, B]] =
      zoomOpt(optional.getOption)(optional.modify)

    def zoom[B](prism: Prism[A, B]): Reuse[ViewListF[F, B]] = zoomOpt(prism.getOption)(prism.modify)

    def zoom[B](traversal: Traversal[A, B]): Reuse[ViewListF[F, B]] =
      zoomList(traversal.getAll)(traversal.modify)

    def withOnMod(f: List[A] => F[Unit]): Reuse[ViewListF[F, A]] = rvl.map(_.withOnMod(f))

    def widen[B >: A]: Reuse[ViewListF[F, B]] = rvl.map(_.widen[B])
  }
}
