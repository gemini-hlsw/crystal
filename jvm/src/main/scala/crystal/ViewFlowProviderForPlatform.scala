package crystal

import cats.effect.ConcurrentEffect
import scala.language.higherKinds

trait ViewFlowProviderForPlatform extends ViewFlowProvider {
  type Flow[A] = Nothing

  def flow[F[_] : ConcurrentEffect, A](view: ViewRO[F, A]): Flow[A] = throw new Exception("Flows are not supported in JVM")
}