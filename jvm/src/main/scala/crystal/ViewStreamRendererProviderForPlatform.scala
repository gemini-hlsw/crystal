package crystal

import cats.effect.ConcurrentEffect
import scala.language.higherKinds

trait ViewStreamRendererProviderForPlatform extends ViewStreamRendererProvider {
  type StreamRenderer[A] = Nothing
  type StreamRendererMod[A] = Nothing

  def streamRenderer[F[_] : ConcurrentEffect, A](view: ViewRO[F, A]): StreamRenderer[A] = 
    throw new Exception("StreamRenderer is not supported in JVM")
}