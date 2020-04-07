package crystal

import cats.effect.ConcurrentEffect
import scala.language.higherKinds

trait ComponentTypesForPlatform extends ComponentTypes {
  type StreamRenderer[A] = react.StreamRenderer.ReactStreamRendererComponent[A]
  type StreamRendererMod[F[_], A] = react.StreamRendererMod.ReactStreamRendererComponent[F, A]
  type AppRoot[F[_], C, M] = react.AppRoot.ReactAppRootComponent[F, C, M]
}
