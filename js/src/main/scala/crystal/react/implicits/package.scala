package crystal.react

import crystal._
import cats.syntax.all._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.component.Generic.MountedSimple

import scala.util.control.NonFatal
import cats.effect.Sync
import cats.effect.Async
import cats.effect.Effect
import cats.effect.SyncIO
import cats.effect.implicits._
import monocle.Lens
import org.typelevel.log4cats.Logger

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

    /** Like `setState` but completes with a `Unit` value *after* the state modification has
      * been completed. In contrast, `setState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access only to state.
      */
    def setStateIn[F[_]: Async](s: S): F[Unit] =
      Async[F].async_[Unit] { cb =>
        val doMod = self.setState(s, Callback(cb(Right(()))))
        doMod
          .maybeHandleError { case NonFatal(t) =>
            Callback(cb(Left(t)))
          }
          .runNow()
      }

    /** Like `modState` but completes with a `Unit` value *after* the state modification has
      * been completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access only to state.
      */
    def modStateIn[F[_]: Async](mod: S => S): F[Unit] =
      Async[F].async_[Unit] { cb =>
        val doMod = self.modState(mod, Callback(cb(Right(()))))
        doMod
          .maybeHandleError { case NonFatal(t) =>
            Callback(cb(Left(t)))
          }
          .runNow()
      }

    def setStateLIn[F[_]]: SetStateLApplied[F, S] =
      new SetStateLApplied[F, S](self)

    def modStateLIn[F[_]]: ModStateLApplied[F, S] =
      new ModStateLApplied[F, S](self)
  }

  implicit class ModStateWithPropsFOps[S, P](
    private val self: StateAccess.WriteWithProps[CallbackTo, P, S]
  ) extends AnyVal {

    /** Like `modState` but completes with a `Unit` value *after* the state modification has
      * been completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access to both state and props.
      */
    def modStateWithPropsIn[F[_]: Async](
      mod: (S, P) => S
    ): F[Unit] =
      Async[F].async_[Unit] { cb =>
        val doMod = self.modState(mod, Callback(cb(Right(()))))
        doMod
          .maybeHandleError { case NonFatal(t) =>
            Callback(cb(Left(t)))
          }
          .runNow()
      }
  }

  implicit class EffectAOps[F[_], A](private val self: F[A]) extends AnyVal {

    /** Return a `Callback` that will run the effect `F[A]` asynchronously.
      *
      * @param cb Result handler returning a `F[Unit]`.
      */
    def runAsyncCB(
      cb:         Either[Throwable, A] => F[Unit]
    )(implicit F: Effect[F]): Callback =
      CallbackTo.lift(() => self.runAsync(r => cb(r).toIO).unsafeRunSync())

    /** Return a `Callback` that will run the effect `F[A]` asynchronously.
      *
      * @param cb Result handler returning a `Callback`.
      */
    def runAsyncAndThenCB(
      cb:         Either[Throwable, A] => Callback
    )(implicit F: Effect[F]): Callback =
      runAsyncCB(cb.andThen(c => F.delay(c.runNow())))

    /** Return a `Callback` that will run the effect `F[A]` asynchronously and discard the result or errors. */
    def runAsyncAndForgetCB(implicit F: Effect[F]): Callback =
      self.runAsyncCB(_ => F.unit)
  }

  implicit class EffectUnitOps[F[_]](private val self: F[Unit]) extends AnyVal {

    /** Return a `Callback` that will run the effect `F[Unit]` asynchronously and log possible errors.
      *
      * @param cb `Callback` to run in case of success.
      */
    def runAsyncAndThenCB(
      cb:         Callback,
      errorMsg:   String = "Error in F[Unit].runAsyncAndThenCB"
    )(implicit F: Effect[F], logger: Logger[F]): Callback =
      new EffectAOps(self).runAsyncCB {
        case Right(()) => F.delay(cb.runNow())
        case Left(t)   => logger.error(t)(errorMsg)
      }

    /** Return a `Callback` that will run the effect F[Unit] asynchronously and log possible errors. */
    def runAsyncCB(
      errorMsg:   String = "Error in F[Unit].runAsyncCB"
    )(implicit F: Effect[F], logger: Logger[F]): Callback =
      runAsyncAndThenCB(Callback.empty, errorMsg)

    def runAsyncCB(implicit F: Effect[F], logger: Logger[F]): Callback =
      runAsyncCB()
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

  implicit class ViewFModuleOps(private val viewFModule: ViewF.type) extends AnyVal {
    def fromState[F[_]]: FromStateViewF[F] =
      new FromStateViewF[F]()
  }
}

package implicits {
  protected class SetStateLApplied[F[_], S](private val self: StateAccess.Write[CallbackTo, S])
      extends AnyVal     {
    @inline def apply[A, B](lens: Lens[S, B])(a: A)(implicit conv: A => B, F: Async[F]): F[Unit] =
      self.modStateIn(lens.set(a))
  }

  protected class ModStateLApplied[F[_], S](private val self: StateAccess.Write[CallbackTo, S])
      extends AnyVal {
    @inline def apply[A](lens: Lens[S, A])(f: A => A)(implicit F: Async[F]): F[Unit] =
      self.modStateIn(lens.modify(f))
  }
}
