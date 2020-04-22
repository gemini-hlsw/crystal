package crystal.react

import cats.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import crystal.ViewCtx
import implicits._

import cats.kernel.Monoid
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
        model: M,
        ctx: C
    )(
        render: ViewCtx[F, C, M] => VdomNode,
        onUnmount: Option[F[Unit]] = None
    )(
        implicit reusability: Reusability[M],
        effect: Effect[F],
        monoidF: Monoid[F[Unit]]
    ): Component[M] =
      ScalaComponent
        .builder[Unit]("AppRoot")
        .initialState(model)
        .render($ => render(ViewCtx(View($.state, $.modStateIn[F]), ctx)))
        .componentWillUnmount(_ => onUnmount.orEmpty.runInCB)
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
