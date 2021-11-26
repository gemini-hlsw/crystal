package crystal.react

import cats.effect.Fiber

package object hooks
    extends UseSingleEffect.HooksApiExt
    with UseSerialState.HooksApiExt
    with UseStateView.HooksApiExt
    with UseSerialStateView.HooksApiExt {
  type UseSingleEffectLatch[F[_]] = Fiber[F, Throwable, Unit]
}
