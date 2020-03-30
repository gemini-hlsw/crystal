package crystal.react

import cats.effect._
import cats.implicits._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger
import _root_.io.chrisdavenport.log4cats.log4s.Log4sLogger

import scala.scalajs.js

import scala.language.higherKinds

object StreamRendererMod {
  type ModState[A] = (A => A) => Callback
  type ReactStreamRendererProps[A] = (A, ModState[A]) => VdomNode
  type ReactStreamRendererComponent[A] = 
    CtorType.Props[ReactStreamRendererProps[A], UnmountedWithRoot[ReactStreamRendererProps[A], _, _, _]]

  type State[A] = Option[A]

  implicit val logger = Log4sLogger.createLocal[IO]

  def build[F[_] : ConcurrentEffect, A](
      stream: fs2.Stream[F, A],
      reusability: Reusability[A] = Reusability.by_==[A],
      key: js.UndefOr[js.Any] = js.undefined
    ): ReactStreamRendererComponent[A] = {
      implicit val propsReuse: Reusability[ReactStreamRendererProps[A]] = Reusability.byRef
      implicit val aReuse: Reusability[A] = reusability

      class Backend($: BackendScope[ReactStreamRendererProps[A], State[A]]) {

        var cancelToken: Option[CancelToken[F]] = None

        val evalCancellable: SyncIO[CancelToken[F]] =
          ConcurrentEffect[F].runCancelable(
            stream
              .evalMap(v => Sync[F].delay($.setState(Some(v)).runNow()))
              .compile.drain
          )(_.swap.toOption.foldMap(e => Logger[IO].error(e)("[StreamRendererMod] Error on stream")))

        def willMount = Callback {
          cancelToken = Some(evalCancellable.unsafeRunSync())
        }

        def willUnmount = Callback { // Cancellation must be async. Is there a more elegant way of doing this?
          cancelToken.foreach(token => Effect[F].toIO(token).unsafeRunAsyncAndForget())
        }

        def render(props: ReactStreamRendererProps[A], state: Option[A]): VdomNode = 
          state.fold(VdomNode(null))(s => props(s, f => $.modState(_.map(f))))
      }

      ScalaComponent
        .builder[ReactStreamRendererProps[A]]("StreamRendererMod")
        .initialState(Option.empty[A])
        .renderBackend[Backend]
        .componentWillMount(_.backend.willMount)
        .componentWillUnmount(_.backend.willUnmount)
        .configure(Reusability.shouldComponentUpdate)
        .build
        .withRawProp("key", key)
  }
}
