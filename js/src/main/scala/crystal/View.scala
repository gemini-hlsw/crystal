package crystal

import cats.effect.ConcurrentEffect
import cats.implicits._
import crystal.react.Flow
import crystal.react.Flow.ReactFlowComponent
import fs2._
import monocle.Lens

import scala.language.higherKinds

// ConcurrentEffect is needed by Flow, to .runCancelable the stream.
sealed class ViewRO[F[_] : ConcurrentEffect, A](val get: F[A], val stream: Stream[F, A]) {
  lazy val flow: ReactFlowComponent[A] = Flow.flow(stream)

  // Useful for getting an algebra already in F[_].
  def algebra[H[_[_]]](implicit algebra: H[F]): H[F] = algebra

  // map takes any function. We lose access to the model and cannot write to it anymore. Hence ViewRO.
  def map[B](f: A => B): ViewRO[F, B] = {
    new ViewRO(get.map(f), stream.map(f).filterWithPrevious(_ != _))
  }
}

class View[F[_] : ConcurrentEffect, A](fixedLens: FixedLens[F, A], stream: Stream[F, A])
  extends ViewRO[F, A](fixedLens.get, stream) with FixedLens[F, A] {

  // Convenience delegates for FixedLens
  override def set(value: A): F[Unit] =
    fixedLens.set(value)

  override def modify(f: A => A): F[Unit] =
    fixedLens.modify(f)

  override protected[crystal] def compose[B](otherLens: Lens[A, B]): FixedLens[F, B] =
    throw new Exception("Views cannot be composed with other lenses. Try .zoom or .map instead.")

  // zoom takes a lens, we can continue writing to the model.
  def zoom[B](otherLens: Lens[A, B]): View[F, B] = {
    new View(fixedLens compose otherLens, stream.map(otherLens.get).filterWithPrevious(_ != _))
  }
}


