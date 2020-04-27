package crystal

import monocle.Lens
import monocle.Optional
import monocle.Prism

case class ViewOptCtx[F[_], C, A](viewOpt: ViewOpt[F, A], ctx: C) {
  @inline def getOption: Option[A] = viewOpt.getOption

  @inline def set(a: A): F[Unit] = viewOpt.set(a)

  @inline def mod(f: A => A): F[Unit] = viewOpt.mod(f)

  def zoom[B](f: A => B)(g: (B => B) => A => A): ViewOptCtx[F, C, B] =
    ViewOptCtx(viewOpt.zoom(f)(g), ctx)

  def zoomL[B](lens: Lens[A, B]): ViewOptCtx[F, C, B] =
    ViewOptCtx(viewOpt.zoomL(lens), ctx)

  def zoomO[B](optional: Optional[A, B]): ViewOptCtx[F, C, B] =
    ViewOptCtx(viewOpt.zoomO(optional), ctx)

  def zoomP[B](prism: Prism[A, B]): ViewOptCtx[F, C, B] =
    ViewOptCtx(viewOpt.zoomP(prism), ctx)

  def interpreter[I[_[_]]](f: C => ActionInterpreterOpt[F, I, A]): I[F] =
    f(ctx).of(viewOpt)
}

case class ViewCtx[F[_], C, A](view: View[F, A], ctx: C) {
  @inline def get: A = view.get

  @inline def set(a: A): F[Unit] = view.set(a)

  @inline def mod(f: A => A): F[Unit] = view.mod(f)

  def zoom[B](f: A => B)(g: (B => B) => A => A): ViewCtx[F, C, B] =
    ViewCtx(view.zoom(f)(g), ctx)

  def zoomL[B](lens: Lens[A, B]): ViewCtx[F, C, B] =
    ViewCtx(view.zoomL(lens), ctx)

  def zoomO[B](optional: Optional[A, B]): ViewOptCtx[F, C, B] =
    ViewOptCtx(view.zoomO(optional), ctx)

  def zoomP[B](prism: Prism[A, B]): ViewOptCtx[F, C, B] =
    ViewOptCtx(view.zoomP(prism), ctx)

  def interpreter[I[_[_]]](f: C => ActionInterpreter[F, I, A]): I[F] =
    f(ctx).of(view)
}
