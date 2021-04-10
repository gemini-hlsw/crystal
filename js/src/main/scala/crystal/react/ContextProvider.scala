package crystal.react

import crystal.ViewF
import implicits._
import cats.effect.Effect
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object ContextProvider {

  @inline def apply[F[_]] = new Apply[F]

  class Apply[F[_]] {
    def apply[C](ctx: Ctx[F, C], initCtx: C)(
      render:         VdomNode
    )(implicit
      reusabilityC:   Reusability[C],
      effect:         Effect[F]): StateComponent[C] =
      ScalaComponent
        .builder[Unit]
        .initialState(initCtx)
        .render($ => ctx.provide(ViewF.fromState[F]($))(render))
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
