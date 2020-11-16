package crystal.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import implicits._
import scala.scalajs.js

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
      onMount:     js.UndefOr[ViewF[F, M] => F[Unit]] = js.undefined,
      onUnmount:   js.UndefOr[M => F[Unit]] = js.undefined
    )(implicit
      reusability: Reusability[M],
      effect:      Effect[F],
      cs:          ContextShift[F]
    ): Component[M] =
      ScalaComponent
        .builder[Unit]
        .initialState(model)
        .render($ => render(ViewF.fromState($)))
        .componentDidMount($ => onMount.toOption.map(_(ViewF.fromState($)).runAsyncCB).getOrEmpty)
        .componentWillUnmount($ => onUnmount.toOption.map(_($.state).runAsyncCB).getOrEmpty)
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
