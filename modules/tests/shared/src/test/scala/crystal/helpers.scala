// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Eq
import cats.FlatMap
import cats.data.Chain
import cats.effect.Ref
import cats.effect.SyncIO
import cats.effect.Temporal
import cats.syntax.all.*
import monocle.Iso
import monocle.Lens
import monocle.Optional
import monocle.Traversal
import monocle.macros.GenLens
import monocle.std.option.some

import scala.concurrent.duration.*

case class Wrap[A](a: A) {
  def map[B](f: A => B): Wrap[B] = Wrap(f(a))
}
object Wrap              {
  def a[A]: Lens[Wrap[A], A] = GenLens[Wrap[A]](_.a)

  given [A: Eq]: Eq[Wrap[A]] = Eq.by(_.a)

  def iso[A]: Iso[Wrap[A], A] = Iso[Wrap[A], A](_.a)(Wrap.apply)
}

case class WrapOpt[A](a: Option[A])
object WrapOpt {
  def a[A]: Lens[WrapOpt[A], Option[A]] = GenLens[WrapOpt[A]](_.a)

  def aOpt[A]: Optional[WrapOpt[A], A] = WrapOpt.a.andThen(some[A].asOptional)

  given [A: Eq]: Eq[WrapOpt[A]] = Eq.by(_.a)
}

case class WrapList[A](a: List[A])
object WrapList {
  def a[A]: Lens[WrapList[A], List[A]] = GenLens[WrapList[A]](_.a)

  def aList[A]: Traversal[WrapList[A], A] = WrapList.a.andThen(Traversal.fromTraverse[List, A])

  given [A: Eq]: Eq[WrapList[A]] = Eq.by(_.a)
}

// Builds a `modCB` function for a `View` backed by a `Ref`.
private def refModCB[F[_]: FlatMap, A](ref: Ref[F, A]): (A => A, (A, A) => F[Unit]) => F[Unit] =
  (f, cb) =>
    ref
      .modify[(A, A)]: previous =>
        val current = f(previous)
        (current, (previous, current))
      .flatMap: (previous, current) =>
        cb(previous, current)

class AccumulatingViewF[F[_]: Temporal, A] private (
  a:        A,
  ref:      Ref[F, A],
  accumRef: Ref[F, Chain[(A, FiniteDuration)]]
) extends ViewF[F, A](
      a, // Should not be used, will stay the same.
      (mod, cb) =>
        refModCB[F, A](ref).apply(
          mod,
          (previous, current) =>
            Temporal[F].realTime
              .tupleLeft(current)
              .flatMap: accum =>
                accumRef.update(_.append(accum)) >> cb(previous, current)
        )
    ):
  def accumulated: F[Chain[(A, FiniteDuration)]] = accumRef.get

object AccumulatingViewF:
  def of[F[_]: Temporal, A](value: A): F[AccumulatingViewF[F, A]] =
    for
      ref      <- Ref.of[F, A](value)
      accumRef <- Ref.of[F, Chain[(A, FiniteDuration)]](Chain((value, 0.millis)))
    yield AccumulatingViewF(value, ref, accumRef)

object SyncIORefView:
  def of[A](value: A): ViewF[SyncIO, A] =
    val ref: Ref[SyncIO, A] = Ref.unsafe(value)
    new ViewF[SyncIO, A](ref.get.unsafeRunSync(), refModCB(ref))
