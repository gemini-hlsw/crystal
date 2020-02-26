package crystal

import cats.implicits._
import cats.effect.{ConcurrentEffect, Sync, SyncIO, Timer}
import fs2._
import fs2.concurrent.SignallingRef
import monocle.Lens

import scala.language.higherKinds

case class Model[F[_] : ConcurrentEffect : Timer, M](modelRef: SignallingRef[F, M]) {
  val stream: Stream[F, M] = modelRef.discrete

  def view[A](lens: Lens[M, A] = Lens.id[M]): View[F, A] = {
    val fixedLens = FixedLens.fromLensAndModelRef(lens, modelRef)
    // Should we drop duplicates by Eq? Or leave it to the caller?
    new View(fixedLens, stream.map(lens.get).filterWithPrevious(_ != _))
  }
}

object Model {
  final class ApplyBuilders[G[_], F[_]](val GFT: (Sync[G], ConcurrentEffect[F], Timer[F])) extends AnyVal {
    private implicit def SyncG = GFT._1
    private implicit def ConcurrentEffectF = GFT._2
    private implicit def TimerF = GFT._3

    def of[M](model: M): G[Model[F, M]] = 
      SignallingRef.in[G, F, M](model).map(ref => Model(ref))
  }

  def in[G[_], F[_]](implicit G: Sync[G], F: ConcurrentEffect[F], T: Timer[F]) =
    new ApplyBuilders[G, F]((G, F, T))

  def apply[F[_]](implicit F: ConcurrentEffect[F], T: Timer[F]) =
    in[F, F]
}
