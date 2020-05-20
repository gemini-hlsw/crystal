package crystal.react

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import crystal.Ctx
import japgolly.scalajs.react.extra.StateSnapshot

object AppRoot {
  type Component[M] =
    ScalaComponent[
      Unit,
      M,
      Unit,
      CtorType.Nullary
    ]

  def apply[F[_]] = new Apply[F]

  class Backend[M, C](ctx: C, renderFn: Ctx[C, StateSnapshot[M]] => VdomNode)(
    $                     : BackendScope[Unit, M]
  )(implicit
    reusability:           Reusability[M]
  ) {
    private val stateSnapshot = StateSnapshot.withReuse.prepareVia($)

    def render(state: M): VdomNode =
      renderFn(
        Ctx(stateSnapshot(state), ctx)
      )
  }

  class Apply[F[_]] {
    def apply[M, C](
      model:       M,
      ctx:         C
    )(
      render:      Ctx[C, StateSnapshot[M]] => VdomNode,
      onUnmount:   Option[Callback] = None
    )(implicit
      reusability: Reusability[M]
    ) = //: Component[M] =
      ScalaComponent
        .builder[Unit]
        .initialState(model)
        .backend($ => new Backend[M, C](ctx, render)($))
        .renderBackend
        .componentWillUnmount(_ => onUnmount.getOrEmpty)
        .configure(Reusability.shouldComponentUpdate)
        .build
  }
}
