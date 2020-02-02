package crystal.react

import cats.effect.{CancelToken, ConcurrentEffect, Effect, IO, Sync, SyncIO}
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import scala.scalajs.js

import scala.language.higherKinds

object StreamRenderer {
  type ReactStreamRendererProps[A] = A => VdomNode
  type ReactStreamRendererComponent[A] = 
    CtorType.Props[ReactStreamRendererProps[A], UnmountedWithRoot[ReactStreamRendererProps[A], _, _, _]]

  type State[A] = Option[A] // Use Pot or something else that can hold errors?

  // We should let pass Reusability[A] somewhere (or provide it in a View).

  def build[F[_] : ConcurrentEffect, A](stream: fs2.Stream[F, A], key: js.UndefOr[js.Any] = js.undefined): ReactStreamRendererComponent[A] = {

    class Backend($: BackendScope[ReactStreamRendererProps[A], State[A]]) {

      var cancelToken: Option[CancelToken[F]] = None

      val evalCancellable: SyncIO[CancelToken[F]] =
        ConcurrentEffect[F].runCancelable(
          stream
            .evalMap(v => Sync[F].delay($.setState(Some(v)).runNow()))
            .compile.drain
        )(_ match {
              case Left(e) => IO(e.printStackTrace()) // If the stream ends in error, we print to console.
              case _ => IO.unit
          }
        )


      def willMount = Callback {
        cancelToken = Some(evalCancellable.unsafeRunSync())
      }

      def willUnmount = Callback { // Cancellation must be async. Is there a more elegant way of doing this?
        cancelToken.foreach(token => Effect[F].toIO(token).unsafeRunAsyncAndForget())
      }

      def render(props: ReactStreamRendererProps[A], state: Option[A]): VdomNode = state.fold(VdomNode(null))(props)
    }

    ScalaComponent
      .builder[ReactStreamRendererProps[A]]("FlowWrapper")
      .initialState(Option.empty[A])
      .renderBackend[Backend]
      .componentWillMount(_.backend.willMount)
      .componentWillUnmount(_.backend.willUnmount)
      .shouldComponentUpdatePure(scope => (scope.currentState ne scope.nextState) || (scope.currentProps ne scope.nextProps))
      .build
      .withRawProp("key", key)
  }
}
