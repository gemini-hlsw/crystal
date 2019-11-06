package com.rpiaggio.crystal

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import vdom.html_<^._
import cats.effect._
import cats.implicits._
import fs2.concurrent.SignallingRef
import scala.language.higherKinds

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

object Flow {
  type ReactFlowProps[A] = Option[A] => VdomElement
  type ReactFlowComponent[A] = CtorType.Props[ReactFlowProps[A], UnmountedWithRoot[ReactFlowProps[A], _, _, _]]

  type State[A] = Option[A] // Use Pot or something else that can hold errors

  def flow[F[_] : ConcurrentEffect : Timer, A](stream: fs2.Stream[F, A], key: js.UndefOr[js.Any] = js.undefined): ReactFlowComponent[A] = {

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


      def render(pr: ReactFlowProps[A], v: Option[A]): VdomElement =
        <.div(
          pr(v),
          <.button(^.tpe := "button", "STOP!", ^.onClick --> willUnmount)
        )
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