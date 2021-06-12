package crystal.react

import crystal.ViewF
import implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import cats.effect.SyncIO
import crystal.react.reuse.Reuse

object StateProviderSyncIO {

  class Backend[M]($ : BackendScope[Reuse[ViewF[SyncIO, M] => VdomNode], M]) {
    def render(props: Reuse[ViewF[SyncIO, M] => VdomNode]) =
      props(ViewF.fromStateSyncIO($))
  }

  def apply[M](model: M)(implicit reusabilityM: Reusability[M]): StateComponent[M] =
    ScalaComponent
      .builder[Reuse[ViewF[SyncIO, M] => VdomNode]]
      .initialState(model)
      .backend($ => new Backend($))
      .renderBackend
      .configure(Reusability.shouldComponentUpdate)
      .build
}
