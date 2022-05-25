package crystal.react

import cats.effect.Fiber
import cats.effect.FiberIO
import cats.effect.Resource
import crystal.Pot
import crystal.implicits._
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
    with UseResource.HooksApiExt
    with UseStream.HooksApiExt
    with UseStreamResource.HooksApiExt
    with UseStreamView.HooksApiExt
    with UseStreamResourceView.HooksApiExt
    with UseStreamViewWithReuse.HooksApiExt
    with UseStreamResourceViewWithReuse.HooksApiExt {
  type UseSingleEffectLatch[F[_]] = Fiber[F, Throwable, Unit]

  protected[hooks] type NeverReuse = Reuse[Unit]
  protected[hooks] val NeverReuse: NeverReuse = ().reuseNever

  protected[hooks] def streamEvaluationResource[A](
    stream: fs2.Stream[DefaultA, A],
    setPot: Pot[A] => DefaultA[Unit]
  ): Resource[DefaultA, FiberIO[Unit]] =
    Resource.make(
      stream
        .evalMap(setPot.compose(_.ready))
        .compile
        .drain
        .handleErrorWith(setPot.compose(Pot.error))
        .start
    )(_.cancel)
}
