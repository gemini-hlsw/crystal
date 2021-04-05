package crystal.react

import crystal.ViewF
import implicits._
import cats.effect.Effect
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object StateProvider {
  @inline def apply[F[_]] = new Apply[F]

  class Apply[F[_]] {
    def apply[M](model: M)(
      render:           ViewF[F, M] => VdomNode
    )(implicit
      reusabilityM:     Reusability[M],
      effect:           Effect[F]): StateComponent[M] =
      ScalaComponent
        .builder[Unit]
        .initialState(model)
        .render($ => render(ViewF.fromState($)))
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
