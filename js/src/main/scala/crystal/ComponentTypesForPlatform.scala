package crystal

import cats.effect.ConcurrentEffect
import scala.language.higherKinds

trait ComponentTypesForPlatform extends ComponentTypes {
  type StreamRenderer[A] = react.StreamRenderer.Component[A]
  type StreamRendererMod[F[_], A] = react.StreamRendererMod.Component[F, A]
  type AppRoot[F[_], C, M] = react.AppRoot.Component[F, C, M]
}
