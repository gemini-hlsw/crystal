package crystal

trait ComponentTypesForPlatform extends ComponentTypes {
  type StreamRenderer[A]    = Nothing
  type StreamRendererMod[A] = Nothing
}
