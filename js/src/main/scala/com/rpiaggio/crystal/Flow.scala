package com.rpiaggio.crystal

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import vdom.html_<^._
import cats.effect._
import cats.implicits._
import fs2.concurrent.SignallingRef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

object Flow {
  type ReactFlowProps[A] = Option[A] => VdomElement
  type ReactFlowComponent[A] = CtorType.Props[ReactFlowProps[A], UnmountedWithRoot[ReactFlowProps[A], _, _, _]]


  implicit val ioTimer = IO.timer
  implicit val ioCS: ContextShift[IO] = IO.contextShift(global)
  //  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect

  def flow[ /*F[_] : Sync,*/ A](stream: fs2.Stream[IO, A], key: js.UndefOr[js.Any] = js.undefined): ReactFlowComponent[A] = {

    class Backend($: BackendScope[ReactFlowProps[A], Option[A]]) {

      val done = SignallingRef[IO, Boolean](false).unsafeRunSync()

      def willMount = Callback {
        stream
          .interruptWhen(done)
          .evalMap(v => Sync[IO].delay($.setState(Some(v)).runNow()))
          .compile.drain
          .unsafeRunAsyncAndForget()
      }

      def willUnmount = Callback {
        done.set(true).unsafeRunSync()
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