package crystal.react

import crystal.ViewF
import crystal.react.reuse.Reuse
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }
import japgolly.scalajs.react.vdom.html_<^._

import implicits._

object StateProvider {

  class Backend[M]($ : BackendScope[Reuse[ViewF[DefaultS, M] => VdomNode], M]) {
    def render(props: Reuse[ViewF[DefaultS, M] => VdomNode]) =
      props(ViewF.fromState($))
  }

  def apply[M](model: M)(implicit
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
