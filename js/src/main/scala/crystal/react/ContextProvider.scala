package crystal.react

import crystal.ViewF
import implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import cats.effect.SyncIO
import crystal.react.reuse.Reuse

object ContextProviderSyncIO {

  class Backend[C](ctx: Ctx[SyncIO, C])($ : BackendScope[Reuse[VdomNode], C]) {
    def render(props: Reuse[VdomNode]) =
      ctx.provide(ViewF.fromStateSyncIO($))(props)
  }

  def apply[C](ctx: Ctx[SyncIO, C], initCtx: C)(implicit
    reusabilityC:   Reusability[C]
  ): ContextComponent[C, Backend[C]] =
    ScalaComponent
      .builder[Reuse[VdomNode]]
      .initialState(initCtx)
      .backend($ => new Backend(ctx)($))
      .renderBackend
      .configure(Reusability.shouldComponentUpdate)
      .build
}
