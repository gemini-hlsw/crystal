package crystal

import cats.effect.{ConcurrentEffect, SyncIO, Timer}
import fs2._
import fs2.concurrent.SignallingRef
import monocle.Lens

import scala.language.higherKinds

case class Model[F[_] : ConcurrentEffect : Timer, M](initialModel: M) {
  private val modelRef: SignallingRef[F, M] =  SignallingRef.in[SyncIO, F, M](initialModel).unsafeRunSync()

  private val stream: Stream[F, M] = modelRef.discrete

  def view[A](lens: Lens[M, A]): View[F, A] = {
    val fixedLens = FixedLens.fromLensAndModelRef(lens, modelRef)
    // Do we have to split the stream here or pass the same one? Topic?
    // Also: do we have to watch out for resource leaks?
    // We should drop duplicates by Eq?
    // Or use reusability here?
    new View(fixedLens, stream.map(lens.get).filterWithPrevious(_ != _))
  }
}
