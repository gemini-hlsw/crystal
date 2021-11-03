package crystal.react

import crystal.ViewF
import implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }
import crystal.react.reuse.Reuse
import cats.Monad

object StateProvider {

  class Backend[M]($ : BackendScope[Reuse[ViewF[DefaultS, M] => VdomNode], M])(implicit
    DefaultS:          Monad[DefaultS]
  ) {
    def render(props: Reuse[ViewF[DefaultS, M] => VdomNode]) =
      props(ViewF.fromState($))
  }

  def apply[M](model: M)(implicit
    DefaultS:         Monad[DefaultS],
    reusabilityM:     Reusability[M]
  ): StateComponent[M, Backend[M]] =
    ScalaComponent
      .builder[Reuse[ViewF[DefaultS, M] => VdomNode]]
      .initialState(model)
      .backend($ => new Backend($))
      .renderBackend
      .configure(Reusability.shouldComponentUpdate)
      .build
}
