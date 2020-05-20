package crystal

import monocle.Lens
import monocle.Optional
import monocle.Prism
import cats.Functor
import cats.kernel.Eq
import cats.implicits._

// Ctx encapsulates a fixed context and a regular value.
case class Ctx[C, +A](value: A, ctx: C) {
  def map[B](f: A => B): Ctx[C, B] = Ctx(f(value), ctx)

  // In scala 3, let's define "f: implicit C => B"
  def withCtx[B](f: C => B): B = f(ctx)

  // In scala 3, let's define "f: (A, implicit C) => B"
  def withCtxA[B](f: (A, C) => B): B = f(value, ctx)
}

object Ctx {
  implicit def eqCtx[C: Eq, A: Eq]: Eq[Ctx[C, A]] = Eq.by(c => (c.ctx, c.value))

  implicit def ctxFunctor[C, *]: Functor[Ctx[C, *]] = // TODO Functor tests
    new Functor[Ctx[C, *]] {
      override def map[A, B](fa: Ctx[C, A])(f: A => B): Ctx[C, B] = fa.map(f)
    }

  implicit class ViewOptCtxOps[F[_], C, A](val ctx: Ctx[C, ViewOpt[F, A]]) extends AnyVal {
    @inline def getOption: Option[A] = ctx.value.getOption

    @inline def set(a: A): F[Unit] = ctx.value.set(a)

    @inline def mod(f: A => A): F[Unit] = ctx.value.mod(f)

    def zoom[B](f: A => B)(g: (B => B) => A => A): Ctx[C, ViewOpt[F, B]] =
      ctx.map(_.zoom(f)(g))

    def zoomL[B](lens: Lens[A, B]): Ctx[C, ViewOpt[F, B]] =
      ctx.map(_.zoomL(lens))

    def zoomO[B](optional: Optional[A, B]): Ctx[C, ViewOpt[F, B]] =
      ctx.map(_.zoomO(optional))

    def zoomP[B](prism: Prism[A, B]): Ctx[C, ViewOpt[F, B]] =
      ctx.map(_.zoomP(prism))

    def interpreter[I[_[_]]](f: C => ActionInterpreterOpt[F, I, A]): I[F] =
      f(ctx.ctx).of(ctx.value)
  }

  implicit class ViewCtxOps[F[_], C, A](val ctx: Ctx[C, View[F, A]]) extends AnyVal {
    @inline def get: A = ctx.value.get

    def zoom[B](f: A => B)(g: (B => B) => A => A): Ctx[C, View[F, B]] =
      ctx.map(_.zoom(f)(g))

    def zoomL[B](lens: Lens[A, B]): Ctx[C, View[F, B]] =
      ctx.map(_.zoomL(lens))

    def interpreter[I[_[_]]](f: C => ActionInterpreter[F, I, A]): I[F] =
      f(ctx.ctx).of(ctx.value)
  }

}
