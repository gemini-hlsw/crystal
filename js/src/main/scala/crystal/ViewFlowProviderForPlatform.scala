package crystal

import cats.effect.ConcurrentEffect
import scala.language.higherKinds

trait ViewFlowProviderForPlatform extends ViewFlowProvider {
  type Flow[A] = react.Flow.ReactFlowComponent[A]

  def flow[F[_] : ConcurrentEffect, A](view: ViewRO[F, A]): Flow[A] = react.Flow.flow(view.stream)
}