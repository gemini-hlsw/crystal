package crystal.react

import crystal.ViewF
import cats.effect.Async
import implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object ContextProvider {

  @inline def apply[F[_]] = new Apply[F]

  class Apply[F[_]] {
    def apply[C](ctx:        Ctx[F, C], initCtx:    C)(
      render:                VdomNode
    )(implicit reusabilityC: Reusability[C], async: Async[F]): StateComponent[C] =
      ScalaComponent
        .builder[Unit]
        .initialState(initCtx)
        .render($ => ctx.provide(ViewF.fromState[F]($))(render))
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
