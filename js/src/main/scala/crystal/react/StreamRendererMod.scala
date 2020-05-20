package crystal.react

import implicits._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.{ Ref => _, _ }
import japgolly.scalajs.react.vdom.html_<^._
import _root_.io.chrisdavenport.log4cats.Logger

import scala.scalajs.js

import cats.effect.concurrent.Ref
import scala.concurrent.duration.FiniteDuration
import cats.kernel.Monoid
import crystal.data._
import crystal.data.implicits._
import crystal.data.react.implicits._
import japgolly.scalajs.react.extra.StateSnapshot

object StreamRendererMod {
  type Props[F[_], A]     = StateSnapshot[Pot[A]] => VdomNode
  type Component[F[_], A] =
    CtorType.Props[Props[F, A], UnmountedWithRoot[
      Props[F, A],
      _,
      _,
      _
    ]]

  class Hold[F[_]: ConcurrentEffect: Timer, A](
    setter:      A => F[Unit],
    duration:    Option[FiniteDuration],
    cancelToken: Ref[F, Option[CancelToken[F]]],
    buffer:      Ref[F, Option[A]]
  )(implicit
    monoidF:     Monoid[F[Unit]]
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
          token => // No error handling on purpose. If Hold fails, just do no Hold. There isn't much we can do here.
            cancelToken.set(token.some)
        }

      }.orEmpty
  }

  object Hold {
    def apply[F[_]: ConcurrentEffect: Timer, A](
      setter:   A => F[Unit],
      duration: Option[FiniteDuration]
    )(implicit
      monoidF:  Monoid[F[Unit]]
    ): SyncIO[Hold[F, A]] =
      for {
        cancelToken <- Ref.in[SyncIO, F, Option[CancelToken[F]]](None)
        buffer      <- Ref.in[SyncIO, F, Option[A]](None)
      } yield new Hold(setter, duration, cancelToken, buffer)
  }

  type State[A] = Pot[A]

  def build[F[_]: ConcurrentEffect: Timer: Logger, A](
    stream:       fs2.Stream[F, A],
    reusability:  Reusability[A] = Reusability.by_==[A],
    key:          js.UndefOr[js.Any] = js.undefined,
    holdAfterMod: Option[FiniteDuration] = None
  )(implicit
    monoidF:      Monoid[F[Unit]]
  ): Component[F, A] = {
    implicit val propsReuse: Reusability[Props[F, A]] =
      Reusability.byRef // Should this be Reusable[Props[A]]?
    implicit val aReuse: Reusability[A] = reusability

    class Backend($ : BackendScope[Props[F, A], State[A]]) {

      private var cancelToken: Option[CancelToken[F]] = None

      val hold: Hold[F, Pot[A]] =
        Hold($.setStateIn[F], holdAfterMod).unsafeRunSync()

      private val evalCancellable: SyncIO[CancelToken[F]] =
        ConcurrentEffect[F].runCancelable(
          stream
            .evalMap(v => hold.set(v.ready))
            .compile
            .drain
        )(
          _.swap.toOption.foldMap(t =>
            $.setStateIn[IO](Error(t)) >>
              Effect[F].toIO(Logger[F].error(t)("[StreamRendererMod] Error on stream"))
          )
        )

      def startUpdates =
        Callback {
          cancelToken = Some(evalCancellable.unsafeRunSync())
        }

      def stopUpdates =
        cancelToken.map(_.runInCBAndForget()).getOrEmpty

      private val holdAndModStateSnapshot =
        StateSnapshot.withReuse.prepare[Pot[A]]((optS, cb) =>
          hold.enable.runInCBAndThen($.setStateOption(optS, cb))
        )

      def render(
        props: Props[F, A],
        state: Pot[A]
      ): VdomNode =
        props(
          holdAndModStateSnapshot(state)
        )
    }

    ScalaComponent
      .builder[Props[F, A]]
      .initialState(Pot.pending[A])
      .renderBackend[Backend]
      .componentDidMount(_.backend.startUpdates)
      .componentWillUnmount(_.backend.stopUpdates)
      .configure(Reusability.shouldComponentUpdate)
      .build
      .withRawProp("key", key)
  }
}
