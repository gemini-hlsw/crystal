package crystal.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import implicits._

import crystal.View
import cats.effect.Effect

object AppRoot {
  type Component[M] =
    ScalaComponent[
      Unit,
      M,
      Unit,
      CtorType.Nullary
    ]

  def apply[F[_]] = new Apply[F]

  class Apply[F[_]] {
    def apply[M, C](
      model:       M
    )(
      render:      View[F, M] => VdomNode,
      onUnmount:   Option[F[Unit]] = None
    )(implicit
      reusability: Reusability[M],
      effect:      Effect[F]
    ): Component[M] =
      ScalaComponent
        .builder[Unit]
        .initialState(model)
        .render($ => render(View($.state, $.modStateIn[F])))
        .componentWillUnmount(_ => onUnmount.map(_.runInCB).getOrEmpty)
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
