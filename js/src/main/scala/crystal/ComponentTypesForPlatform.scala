package crystal

import cats.effect.ConcurrentEffect
import scala.language.higherKinds

trait ComponentTypesForPlatform extends ComponentTypes {
  type StreamRenderer[A] = react.StreamRenderer.Component[A]
  type StreamRendererMod[F[_], A] = react.StreamRendererMod.Component[F, A]
  type AppRoot[M] = react.AppRoot.Component[M]
}
