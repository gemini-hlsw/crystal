package crystal.react

import crystal.ViewF
import implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import crystal.react.reuse.Reuse
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

object ContextProvider {

  class Backend[C](ctx: Ctx[DefaultS, C])($ : BackendScope[Reuse[VdomNode], C]) {
    def render(props: Reuse[VdomNode]) =
      ctx.provide(ViewF.fromState($))(props)
  }

  def apply[C](ctx: Ctx[DefaultS, C], initCtx: C)(implicit
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
