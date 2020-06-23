package crystal

import cats.effect.ConcurrentEffect
import japgolly.scalajs.react._
import io.chrisdavenport.log4cats.Logger

package object react {

  type SetState[F[_], A] = A => F[Unit]
  type ModState[F[_], A] = (A => A) => F[Unit]

  implicit class StreamOps[F[_], A](private val s: fs2.Stream[F, A]) {
    def render(implicit
      ce:     ConcurrentEffect[F],
      logger: Logger[F],
      reuse:  Reusability[A]
    ): StreamRenderer.Component[A] =
      StreamRenderer.build(s)
  }

}
