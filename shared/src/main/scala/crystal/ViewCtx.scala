package crystal

import cats.effect.Sync
import monocle.Lens

import scala.language.higherKinds

case class ViewCtx[F[_], C, A](view: View[F, A], ctx: C) {
  @inline def get: A = view.value

  @inline def set(a: A): F[Unit] = view.set(a)

  @inline def mod(f: A => A): F[Unit] = view.mod(f)

  def zoom[B](f: A => B)(g: (B => B) => A => A): ViewCtx[F, C, B] = ViewCtx(view.zoom(f)(g), ctx)

  def zoomL[B](lens: Lens[A, B]): ViewCtx[F, C, B] = ViewCtx(view.zoomL(lens), ctx)

  def interpreter[I[_[_]]](f: C => ActionInterpreter[F, I, A]): I[F] =
    f(ctx).of(view) 
}