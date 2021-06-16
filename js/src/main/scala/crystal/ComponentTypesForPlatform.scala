package crystal

trait ComponentTypesForPlatform extends ComponentTypes {
  type StreamRenderer[A]    = react.StreamRenderer.Component[A]
  type StreamRendererMod[A] = react.StreamRendererMod.Component[A]
}
