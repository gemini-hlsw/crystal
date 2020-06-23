package crystal.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import implicits._

import crystal.ViewF
import cats.effect.Effect
import cats.effect.ContextShift

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
      render:      ViewF[F, M] => VdomNode,
      onUnmount:   Option[F[Unit]] = None
    )(implicit
      reusability: Reusability[M],
      effect:      Effect[F],
      cs:          ContextShift[F]
    ): Component[M] =
      ScalaComponent
        .builder[Unit]
        .initialState(model)
        .render($ => render(ViewF($.state, $.modStateIn[F])))
        .componentWillUnmount(_ => onUnmount.map(_.runInCB).getOrEmpty)
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
