package crystal

import cats.Functor
import cats.effect.concurrent.Ref
import cats.implicits._
import monocle._

import scala.language.higherKinds

// Hides the model type M. Otherwise, we have to declare it in every View.
trait FixedLens[F[_], A] {
  def get: F[A]
  def set(a: A): F[Unit]
  def modify(f: A => A): F[Unit]

  protected[crystal] def compose[B](otherLens: Lens[A, B]): FixedLens[F, B]
}

object FixedLens {
  def fromLensAndModelRef[F[_] : Functor, M, A](lens: Lens[M, A], modelRef: Ref[F, M]): FixedLens[F, A] = new FixedLens[F, A] {
    override def get: F[A] = modelRef.get.map(model => lens.get(model))
    override def set(a: A): F[Unit] = modelRef.update(model => lens.set(a)(model))
    override def modify(f: A => A): F[Unit] = modelRef.update(model => lens.modify(f)(model))

    override protected[crystal] def compose[B](otherLens: Lens[A, B]): FixedLens[F, B] =
      fromLensAndModelRef(lens composeLens otherLens, modelRef)
  }
}