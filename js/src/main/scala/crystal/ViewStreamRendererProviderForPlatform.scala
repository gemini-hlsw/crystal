package crystal

import cats.effect.ConcurrentEffect
import scala.language.higherKinds

trait ViewStreamRendererProviderForPlatform extends ViewStreamRendererProvider {
  type StreamRenderer[A] = react.StreamRenderer.ReactStreamRendererComponent[A]

  def streamRenderer[F[_] : ConcurrentEffect, A](view: ViewRO[F, A]): StreamRenderer[A] = 
    react.StreamRenderer.build(view.stream)
}