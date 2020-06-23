package crystal.react

// import crystal.Pot
// import crystal.Pending
// import crystal.Ready
// import crystal.Error
// import crystal.View
import crystal._
import cats.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.component.Generic.MountedSimple

import scala.util.control.NonFatal
import cats.effect.Sync
import cats.effect.Async
import cats.effect.IO
import cats.effect.Effect
import cats.effect.SyncIO

package object implicits {
  implicit class CallbackToOps[A](private val self: CallbackTo[A]) {
    @inline def to[F[_]: Sync]: F[A] = Sync[F].delay(self.runNow())

    @inline def toStream[F[_]: Sync]: fs2.Stream[F, A] =
      fs2.Stream.eval(self.to[F])
  }

  implicit class ModMountedSimpleFOps[S, P](
    private val self: MountedSimple[CallbackTo, P, S]
  ) extends AnyVal {
    def propsIn[F[_]: Sync]: F[P] = self.props.to[F]
  }

  implicit class StateAccessorFOps[S](
    private val self: StateAccess[CallbackTo, S]
  ) extends AnyVal {

    /** Provides access to state `S` in an `F` */
    def stateIn[F[_]: Sync]: F[S] = self.state.to[F]
  }

  implicit class ModStateFOps[S](
    private val self: StateAccess.Write[CallbackTo, S]
  ) extends AnyVal {
    def setStateIn[F[_]: Async](s: S): F[Unit] =
      Async[F].async[Unit] { cb =>
        val doMod = self.setState(s, Callback(cb(Right(()))))
        doMod
          .maybeHandleError {
            case NonFatal(t) =>
              Callback(cb(Left(t)))
          }
          .runNow()
      }

    /**
      * Like `modState` but completes with a `Unit` value *after* the state modification has
      * been completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access to both state and props.
      */
    def modStateIn[F[_]: Async](mod: S => S): F[Unit] =
      Async[F].async[Unit] { cb =>
        val doMod = self.modState(mod, Callback(cb(Right(()))))
        doMod
          .maybeHandleError {
            case NonFatal(t) =>
              Callback(cb(Left(t)))
          }
          .runNow()
      }
  }

  implicit class ModStateWithPropsFOps[S, P](
    private val self: StateAccess.WriteWithProps[CallbackTo, P, S]
  ) extends AnyVal {

    /**
      * Like `modState` but completes with a `Unit` value *after* the state modification has
      * been completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access to both state and props.
      */
    def modStateWithPropsIn[F[_]: Async](
      mod: (S, P) => S
    ): F[Unit] =
      Async[F].async[Unit] { cb =>
        val doMod = self.modState(mod, Callback(cb(Right(()))))
        doMod
          .maybeHandleError {
            case NonFatal(t) =>
              Callback(cb(Left(t)))
          }
          .runNow()
      }
  }

  implicit class EffectAOps[F[_], A](private val self: F[A]) extends AnyVal {
    def runAsyncInCB(
      cb:              Either[Throwable, A] => IO[Unit]
    )(implicit effect: Effect[F]): Callback =
      CallbackTo.lift(() => Effect[F].runAsync(self)(cb).unsafeRunSync())

    def runInCBAndThen(
      cb:              A => Callback
    )(implicit effect: Effect[F]): Callback =
      runAsyncInCB {
        case Right(a) => cb(a).to[IO]
        case Left(t)  => IO.raiseError(t)
      }

    def runInCBAndForget()(implicit effect: Effect[F]): Callback =
      self.runAsyncInCB(_ => IO.unit)
  }

  implicit class EffectUnitOps[F[_]](private val self: F[Unit]) extends AnyVal {
    def runInCBAndThen(
      cb:                        Callback
    )(implicit effect:           Effect[F]): Callback =
      new EffectAOps(self).runInCBAndThen((_: Unit) => cb)

    def runInCB(implicit effect: Effect[F]): Callback =
      self.runInCBAndForget()
  }

  implicit class SyncIO2Callback[A](private val s: SyncIO[A]) extends AnyVal {
    @inline def toCB: CallbackTo[A] = CallbackTo(s.unsafeRunSync())
  }

  implicit class PotRender[A](val pot: Pot[A]) extends AnyVal {
    def renderPending(f: Long => VdomNode): VdomNode =
      pot match {
        case Pending(start) => f(start)
        case _              => EmptyVdom
      }

    def renderError(f: Throwable => VdomNode): VdomNode =
      pot match {
        case Error(t) => f(t)
        case _        => EmptyVdom
      }

    def renderReady(f: A => VdomNode): VdomNode =
      pot match {
        case Ready(a) => f(a)
        case _        => EmptyVdom
      }
  }

  implicit def throwableReusability: Reusability[Throwable] =
    Reusability.byRef[Throwable]

  implicit def potReusability[A: Reusability](implicit
    throwableReusability: Reusability[Throwable]
  ): Reusability[Pot[A]] =
    Reusability((x, y) =>
      x match {
        case Pending(startx) =>
          y match {
            case Pending(starty) => startx === starty
            case _               => false
          }
        case Error(tx)       =>
          y match {
            case Error(ty) => tx ~=~ ty
            case _         => false
          }
        case Ready(ax)       =>
          y match {
            case Ready(ay) => ax ~=~ ay
            case _         => false
          }
      }
    )

  implicit def viewReusability[F[_], A: Reusability]: Reusability[ViewF[F, A]] =
    Reusability.by(_.get)

  implicit def viewOptReusability[F[_], A: Reusability]: Reusability[ViewOptF[F, A]] =
    Reusability.by(_.get)

  implicit def viewListReusability[F[_], A: Reusability]: Reusability[ViewListF[F, A]] =
    Reusability.by(_.get)
}
