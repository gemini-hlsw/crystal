package crystal

import cats.syntax.all._
import cats.Id
import cats.effect.Sync
import scala.concurrent.Promise
import cats.effect.Async
import monocle.Iso
import monocle.Lens
import monocle.Optional
import monocle.Prism
import monocle.Traversal
import cats.FlatMap

sealed trait ViewOps[F[_], G[_], A] {
  val get: G[A]

  val set: A => F[Unit]

  val mod: (A => A) => F[Unit]

  val modAndGet: (A => A) => F[G[A]]
}

// The difference between a View and a StateSnapshot is that the modifier doesn't act on the current value,
// but passes the modifier function to an external source of truth. Since we are defining no getter
// from such source of truth, a View is defined in terms of a modifier function instead of a setter.
final class ViewF[F[_]: Async: ContextShift, A](val get: A, val mod: (A => A) => F[Unit])
    extends ViewOps[F, Id, A] {

  val set: A => F[Unit] = a => mod(_ => a)

  // In a ViewF, we can derive modAndGet. In ViewOptF and ViewListF we have to pass it, since their
  // mod functions have no idea of the enclosing structure.
  val modAndGet: (A => A) => F[A] = f =>
    Sync[F].delay(Promise[A]()).flatMap { p =>
      mod { a: A =>
        val fa = f(a)
        p.success(fa)
        fa
      } >> Async.fromFuture(Sync[F].delay(p.future))
    }

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ViewF[F, B] =
    new ViewF(
      getB(get),
      (f: B => B) => mod(modB(f))
    )

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(
      getB(get),
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(getB)
    )

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(
      getB(get),
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(getB)
    )

  def as[B](iso: Iso[A, B]): ViewF[F, B] = zoom(iso.asLens)

  def asOpt: ViewOptF[F, A] = zoom(Iso.id[A].asOptional)

  def asList: ViewListF[F, A] = zoom(Iso.id[A].asTraversal)

  def zoom[B](lens: Lens[A, B]): ViewF[F, B] =
    zoom(lens.get _)(lens.modify)

  def zoom[B](
    optional: Optional[A, B]
  ): ViewOptF[F, B] =
    zoomOpt(optional.getOption _)(optional.modify)

  def zoom[B](prism: Prism[A, B]): ViewOptF[F, B] =
    zoomOpt(prism.getOption _)(prism.modify)

  def zoom[B](
    traversal: Traversal[A, B]
  ): ViewListF[F, B] =
    zoomList(traversal.getAll _)(traversal.modify)

  def withOnMod(
    f: A => F[Unit]
  ): ViewF[F, A] =
    new ViewF[F, A](get, modF => modAndGet(modF).flatMap(f))

  override def toString(): String = s"ViewF($get, <modFn>)"
}

object ViewF {
  def apply[F[_]: Async: ContextShift, A](value: A, mod: (A => A) => F[Unit]): ViewF[F, A] =
    new ViewF(value, mod)
}

final class ViewOptF[F[_]: FlatMap, A](
  val get:       Option[A],
  val mod:       (A => A) => F[Unit],
  val modAndGet: (A => A) => F[Option[A]]
) extends ViewOps[F, Option, A] {
  val set: A => F[Unit] = a => mod(_ => a)

  def as[B](iso: Iso[A, B]): ViewOptF[F, B] = zoom(iso.asLens)

  def asList: ViewListF[F, A] = zoom(Iso.id[A].asTraversal)

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(
      get.map(getB),
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(_.map(getB))
    )

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ViewOptF[F, B] =
    new ViewOptF(
      get.flatMap(getB),
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(_.flatMap(getB))
    )

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(
      get.map(getB).orEmpty,
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(_.map(getB).orEmpty)
    )

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

  def withOnMod(
    f: Option[A] => F[Unit]
  ): ViewOptF[F, A] =
    new ViewOptF[F, A](get, modF => modAndGet(modF).flatMap(f), modAndGet)

  override def toString(): String = s"ViewOptF($get, <modFn>)"
}

final class ViewListF[F[_]: FlatMap, A](
  val get:       List[A],
  val mod:       (A => A) => F[Unit],
  val modAndGet: (A => A) => F[List[A]]
) extends ViewOps[F, List, A] {
  val set: A => F[Unit] = a => mod(_ => a)

  def as[B](iso: Iso[A, B]): ViewListF[F, B] = zoom(iso.asLens)

  def zoom[B](getB: A => B)(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(
      get.map(getB),
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(_.map(getB))
    )

  def zoomOpt[B](
    getB: A => Option[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(
      get.flatMap(getB),
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(_.flatMap(getB))
    )

  def zoomList[B](
    getB: A => List[B]
  )(modB: (B => B) => A => A): ViewListF[F, B] =
    new ViewListF(
      get.flatMap(getB),
      (f: B => B) => mod(modB(f)),
      (f: B => B) => modAndGet(modB(f)).map(_.flatMap(getB))
    )

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

  def withOnMod(
    f: List[A] => F[Unit]
  ): ViewListF[F, A] =
    new ViewListF[F, A](get, modF => modAndGet(modF).flatMap(f), modAndGet)

  override def toString(): String = s"ViewListF($get, <modFn>)"
}
