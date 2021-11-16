package crystal.react.hooks

import cats.Monoid
import cats.effect.Async
import cats.effect.Deferred
import cats.effect.Ref
import cats.effect.syntax.all._
import cats.syntax.all._

import scala.concurrent.duration.FiniteDuration

class TimeoutHandle[F[_]](
  duration:   FiniteDuration,
  latch:      Ref[F, Option[Deferred[F, TimeoutHandleLatch[F]]]]
)(implicit F: Async[F], monoid: Monoid[F[Unit]]) {

  private def switchTo(effect: F[Unit]): F[Unit] =
    Deferred[F, TimeoutHandleLatch[F]] >>= (newLatch =>
      latch
        .modify(oldLatch =>
          (newLatch.some,
           for {
             _        <- oldLatch.map(_.get.flatMap(_.cancel)).orEmpty
             newFiber <- effect.start
             _        <- newLatch.complete(newFiber)
           } yield ()
          )
        )
        .flatten
        .uncancelable
    )

  val cancel: F[Unit] = switchTo(F.unit)

  // There's no need to clean up the fiber reference once the effect completes.
  // Worst case scenario, cancel will be called on it, which will do nothing.
  def submit(effect: F[Unit]): F[Unit] = switchTo(effect.delayBy(duration))
}
