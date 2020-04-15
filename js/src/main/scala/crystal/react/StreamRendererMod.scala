package crystal.react

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.{Ref => _, _}
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger
import _root_.io.chrisdavenport.log4cats.log4s.Log4sLogger

import scala.scalajs.js

import cats.effect.concurrent.Ref
import scala.concurrent.duration.FiniteDuration
import crystal.react.implicits._
import crystal.View
import cats.kernel.Monoid

object StreamRendererMod {
  type Props[F[_], A] = View[F, A] => VdomNode
  type Component[F[_], A] =
    CtorType.Props[Props[F, A], UnmountedWithRoot[
      Props[F, A],
      _,
      _,
      _
    ]]

  class Hold[F[_]: ConcurrentEffect: Timer, A](
      setter: A => F[Unit],
      duration: Option[FiniteDuration],
      cancelToken: Ref[F, Option[CancelToken[F]]],
      buffer: Ref[F, Option[A]]
  )(
      implicit monoidF: Monoid[F[Unit]]
  ) {
    def set(a: A): F[Unit] =
      cancelToken.get.flatMap(
        _.fold(setter(a))(_ => buffer.set(a.some))
      )

    private val restart: Option[F[Unit]] =
      duration.map { d =>
        for {
          _ <- (cancelToken.getAndSet(None).flatMap(_.orEmpty)).uncancelable
          _ <- Timer[F].sleep(d)
          _ <- cancelToken.set(None)
          b <- buffer.getAndSet(None)
          _ <- b.map(set).orEmpty
        } yield ()
      }

    val enable: F[Unit] =
      restart.map { r =>
        Sync[F].delay(r.runCancelable(_ => IO.unit).unsafeRunSync()).flatMap {
          token => // TODO Error handling
            cancelToken.set(token.some)
        }
      }.orEmpty
  }

  object Hold {
    def apply[F[_]: ConcurrentEffect: Timer, A](
        setter: A => F[Unit],
        duration: Option[FiniteDuration]
    )(
        implicit monoidF: Monoid[F[Unit]]
    ): SyncIO[Hold[F, A]] =
      for {
        cancelToken <- Ref.in[SyncIO, F, Option[CancelToken[F]]](None)
        buffer <- Ref.in[SyncIO, F, Option[A]](None)
      } yield {
        new Hold(setter, duration, cancelToken, buffer)
      }
  }

  type State[A] = Option[A]

  implicit val logger = Log4sLogger.createLocal[IO]

  def build[F[_]: ConcurrentEffect: Timer, A](
      stream: fs2.Stream[F, A],
      reusability: Reusability[A] = Reusability.by_==[A],
      key: js.UndefOr[js.Any] = js.undefined,
      holdAfterMod: Option[FiniteDuration] = None
  )(
      implicit monoidF: Monoid[F[Unit]]
  ): Component[F, A] = {
    implicit val propsReuse: Reusability[Props[F, A]] =
      Reusability.byRef
    implicit val aReuse: Reusability[A] = reusability

    class Backend($ : BackendScope[Props[F, A], State[A]]) {

      var cancelToken: Option[CancelToken[F]] = None

      val hold: Hold[F, Option[A]] =
        Hold($.setStateIn[F], holdAfterMod).unsafeRunSync()

      val evalCancellable: SyncIO[CancelToken[F]] =
        ConcurrentEffect[F].runCancelable(
          stream
            .evalMap(v => hold.set(v.some))
            .compile
            .drain
        )(
          _.swap.toOption.foldMap(e =>
            Logger[IO].error(e)("[StreamRendererMod] Error on stream")
          )
        )

      def willMount = Callback {
        cancelToken = Some(evalCancellable.unsafeRunSync())
      }

      def willUnmount =
        cancelToken.map(_.startCBAndForget()).getOrEmpty

      def render(
          props: Props[F, A],
          state: Option[A]
      ): VdomNode =
        state.fold(VdomNode(null))(s =>
          props(
            View[F, A](
              s,
              f =>
                hold.enable.flatMap(_ =>
                  $.modStateIn[F]((v: Option[A]) => v.map(f))
                )
            )
          )
        )
    }

    ScalaComponent
      .builder[Props[F, A]]("StreamRendererMod")
      .initialState(Option.empty[A])
      .renderBackend[Backend]
      .componentWillMount(_.backend.willMount)
      .componentWillUnmount(_.backend.willUnmount)
      .configure(Reusability.shouldComponentUpdate)
      .build
      .withRawProp("key", key)
  }
}
