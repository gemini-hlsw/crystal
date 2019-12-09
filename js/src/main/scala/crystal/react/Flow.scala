package crystal.react

import cats.effect.{CancelToken, ConcurrentEffect, Effect, IO, Sync, SyncIO}
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.vdom.html_<^.VdomElement
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import scala.scalajs.js

import scala.language.higherKinds

object Flow {
  type ReactFlowProps[A] = Option[A] => VdomElement
  type ReactFlowComponent[A] = CtorType.Props[ReactFlowProps[A], UnmountedWithRoot[ReactFlowProps[A], _, _, _]]

  type State[A] = Option[A] // Use Pot or something else that can hold errors

  // We should let pass Reusability[A] somewhere (or provide it in a View).

  def flow[F[_] : ConcurrentEffect, A](stream: fs2.Stream[F, A], key: js.UndefOr[js.Any] = js.undefined): ReactFlowComponent[A] = {

    class Backend($: BackendScope[ReactFlowProps[A], State[A]]) {

      var cancelToken: Option[CancelToken[F]] = None // Can we avoid the var?

      val evalCancellable: SyncIO[CancelToken[F]] =
        ConcurrentEffect[F].runCancelable(
          stream
            .evalMap(v => Sync[F].delay($.setState(Some(v)).runNow()))
            .compile.drain
        )(_ => IO.unit) // Handle Errors


      def willMount = Callback {
        cancelToken = Some(evalCancellable.unsafeRunSync())
      }

      def willUnmount = Callback { // Cancellation must be async. Is there a more elegant way of doing this?
        cancelToken.foreach(token => Effect[F].toIO(token).unsafeRunAsyncAndForget())
      }


      def render(pr: ReactFlowProps[A], v: Option[A]): VdomElement = pr(v)
    }

    ScalaComponent
      .builder[ReactFlowProps[A]]("FlowWrapper")
      .initialState(Option.empty[A])
      .renderBackend[Backend]
      .componentWillMount(_.backend.willMount)
      .componentWillUnmount(_.backend.willUnmount)
      .shouldComponentUpdatePure(scope => (scope.currentState ne scope.nextState) || (scope.currentProps ne scope.nextProps))
      .build
      .withRawProp("key", key)
  }
}
