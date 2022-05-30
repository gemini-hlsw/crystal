package crystal.react

import cats.effect.Fiber
import cats.effect.Resource
import crystal.react.reuse._
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }

package object hooks
    extends UseSingleEffect.HooksApiExt
    with UseSerialState.HooksApiExt
    with UseStateCallback.HooksApiExt
    with UseStateView.HooksApiExt
    with UseStateViewWithReuse.HooksApiExt
    with UseSerialStateView.HooksApiExt
    with UseAsyncEffect.HooksApiExt
    with UseEffectResult.HooksApiExt
    with UseResource.HooksApiExt
    with UseStreamResource.HooksApiExt {
  type UnitFiber[F[_]] = Fiber[F, Throwable, Unit]
  type AsyncUnitFiber  = Fiber[DefaultA, Throwable, Unit]

  type StreamResource[A] = Resource[DefaultA, fs2.Stream[DefaultA, A]]

  protected[hooks] type NeverReuse = Reuse[Unit]
  protected[hooks] val NeverReuse: NeverReuse = ().reuseNever
}

package hooks {
  protected[hooks] final case class WithDeps[D, A](deps: D, fromDeps: D => A)

  final case class SyncValue[A](value: A, awaitOpt: Reusable[Option[DefaultA[Unit]]]) {
    def map[B](f: A => B): SyncValue[B] =
      SyncValue(f(value), awaitOpt)
  }

  object SyncValue {
    implicit def reuseSyncValue[A: Reusability]: Reusability[SyncValue[A]] =
      Reusability.by(x => (x.value, x.awaitOpt))
  }

}
