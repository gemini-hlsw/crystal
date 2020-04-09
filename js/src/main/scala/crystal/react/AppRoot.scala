package crystal.react

import cats.implicits._
import cats.effect.ConcurrentEffect
import cats.effect.Timer
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import crystal.ViewCtx
import implicits._

import scala.language.higherKinds
import cats.kernel.Monoid

object AppRoot {
  type Props[F[_], C, M] = ViewCtx[F, C, M] => VdomNode
  type Component[F[_], C, M] =
    ScalaComponent[
      Props[F, C, M],
      Unit,
      Unit,
      CtorType.Props
    ]

  def component[F[_]] = new Apply[F]

  class Apply[F[_]] {
    def apply[M, C](
        model: M,
        ctx: C
    )(
        onUnmount: Option[F[Unit]] = None
    )(
        implicit ce: ConcurrentEffect[F],
        timer: Timer[F],
        monoidF: Monoid[F[Unit]]
    ): Component[F, C, M] = {
      // TODO We don't really need the whole Stream machinery for this. Make a simpler component
      // that just receives an initial state.
      val Renderer = crystal.react.StreamRendererMod
        .build[F, M](fs2.Stream.emit(model).covary[F])

      ScalaComponent
        .builder[Props[F, C, M]]("AppRoot")
        .render_P { props => Renderer(m => props(ViewCtx(m, ctx))) }
        .componentWillUnmount(_ => onUnmount.orEmpty.toCB)
        .build
    }
  }
}
