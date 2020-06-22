package crystal

trait ComponentTypesForPlatform extends ComponentTypes {
  type StreamRenderer[A]          = Nothing
  type StreamRendererMod[F[_], A] = Nothing
  type AppRoot[M]                 = Nothing
}
