package crystal.react

import cats.effect._
import cats.implicits._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.{ Ref => ReactRef, _}
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger
import _root_.io.chrisdavenport.log4cats.log4s.Log4sLogger

import scala.scalajs.js

import scala.language.higherKinds
import cats.effect.concurrent.MVar
import cats.effect.concurrent.Ref
import scala.concurrent.duration.FiniteDuration
import crystal.react.io.implicits._

object StreamRendererMod {
  type ModState[A] = (A => A) => Callback
  type ReactStreamRendererProps[A] = (A, ModState[A]) => VdomNode
  type ReactStreamRendererComponent[A] = 
    CtorType.Props[ReactStreamRendererProps[A], UnmountedWithRoot[ReactStreamRendererProps[A], _, _, _]]

  class Hold[A](
    setter: A => IO[Unit],
    duration: Option[FiniteDuration],
    cancelToken: Ref[IO, Option[CancelToken[IO]]],
    buffer: Ref[IO, Option[A]]
  )(implicit cs: ContextShift[IO], timer: Timer[IO]) {
    def set(a: A): IO[Unit] =
      cancelToken.get.flatMap(
        _.fold(setter(a))(_ => buffer.set(a.some))
      )

    private val restart: Option[IO[Unit]] =
      duration.map{ d =>
        for {
          _ <- (cancelToken.getAndSet(None).flatMap(_.getOrElse(IO.unit))).uncancelable
          _ <- Timer[IO].sleep(d)
          _ <- cancelToken.set(None)
          b <- buffer.getAndSet(None)
          _ <- b.fold(IO.unit)(set)
        } yield ()
      }

    val enable: IO[Unit] =
      restart.fold(IO.unit){
        _.runCancelable(_ => IO.unit).toIO.flatMap{ token =>
          cancelToken.set(token.some)
        }
      }
  }

  object Hold {
    def apply[A](
      setter: A => IO[Unit],
      duration: Option[FiniteDuration]
    )(implicit cs: ContextShift[IO], timer: Timer[IO]): SyncIO[Hold[A]] =
      for {
        cancelToken <- Ref.in[SyncIO, IO, Option[CancelToken[IO]]](None)
        buffer <- Ref.in[SyncIO, IO, Option[A]](None)
      } yield {
        new Hold(setter, duration, cancelToken, buffer)
      }
  }

  type State[A] = Option[A]

  implicit val logger = Log4sLogger.createLocal[IO]

  def build[F[_] : ConcurrentEffect, A](
      stream: fs2.Stream[F, A],
      reusability: Reusability[A] = Reusability.by_==[A],
      key: js.UndefOr[js.Any] = js.undefined,
      holdAfterMod: Option[FiniteDuration] = None
    )(implicit cs: ContextShift[IO], timer: Timer[IO]): ReactStreamRendererComponent[A] = {
      implicit val propsReuse: Reusability[ReactStreamRendererProps[A]] = Reusability.byRef
      implicit val aReuse: Reusability[A] = reusability

      class Backend($: BackendScope[ReactStreamRendererProps[A], State[A]]) {

        var cancelToken: Option[CancelToken[F]] = None

        val hold: Hold[Option[A]] = Hold(a => $.setStateIO(a), holdAfterMod).unsafeRunSync()

        val evalCancellable: SyncIO[CancelToken[F]] =
          ConcurrentEffect[F].runCancelable(
            stream
              .evalMap(v => Sync[F].delay(hold.set(v.some).runNow()))
              .compile.drain
          )(_.swap.toOption.foldMap(e => Logger[IO].error(e)("[StreamRendererMod] Error on stream")))

        def willMount = Callback {
          cancelToken = Some(evalCancellable.unsafeRunSync())
        }

        def willUnmount = Callback { // Cancellation must be async. Is there a more elegant way of doing this?
          cancelToken.foreach(token => Effect[F].toIO(token).unsafeRunAsyncAndForget())
        }

        def render(props: ReactStreamRendererProps[A], state: Option[A]): VdomNode = 
          state.fold(VdomNode(null))(s => props(s, f => hold.enable.flatMap(_ => $.modStateIO(_.map(f)))))
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
