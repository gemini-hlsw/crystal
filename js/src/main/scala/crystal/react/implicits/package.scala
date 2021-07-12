package crystal.react

import crystal._
import cats.MonadError
import cats.syntax.all._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.component.Generic.MountedSimple

import scala.util.control.NonFatal
import cats.effect.Sync
import cats.effect.Async
import cats.effect.SyncIO
import cats.effect.std.Dispatcher
import monocle.Lens
import org.typelevel.log4cats.Logger
import crystal.react.reuse.Reuse

import scalajs.js

package object implicits {
  // implicit class CallbackToOps[A](private val self: CallbackTo[A]) {
  //   @inline def to[F[_]: Sync]: F[A] = Sync[F].delay(self.runNow())
  //
  //   @inline def toStream[F[_]: Sync]: fs2.Stream[F, A] =
  //     fs2.Stream.eval(self.to[F])
  // }
  //
  // implicit class ModMountedSimpleFOps[S, P](
  //   private val self: MountedSimple[CallbackTo, P, S]
  // ) extends AnyVal {
  //   def propsIn[F[_]: Sync]: F[P] = self.props.to[F]
  // }
  //
  // implicit class StateAccessorFOps[S](
  //   private val self: StateAccess[CallbackTo, S]
  // ) extends AnyVal {
  //
  //   /** Provides access to state `S` in an `F` */
  //   def stateIn[F[_]: Sync]: F[S] = self.state.to[F]
  // }
  //
  // implicit class ModStateSyncIOOps[S](
  //   private val self: StateAccess[CallbackTo, S]
  // ) extends AnyVal {
  //   // I can't find a generic way to run a `Sync`, which is needed to execute the callback,
  //   // so these are only available for SyncIO.
  //   def setStateInSyncIO(s: S, cb: S => SyncIO[Unit]): SyncIO[Unit] =
  //     self.setState(s, self.state >>= (r => Callback(cb(r).unsafeRunSync()))).to[SyncIO]
  //
  //   def modStateInSyncIO(f: S => S, cb: S => SyncIO[Unit]): SyncIO[Unit] =
  //     self.modState(f, self.state >>= (r => Callback(cb(r).unsafeRunSync()))).to[SyncIO]
  // }
  //
  implicit class ModStateFOps[F[_], A[_], S](
    private val self: StateAccess.Write[F, A, S]
  ) extends AnyVal {

    def setStateIn(s: S): F[Unit]      = self.setState(s)
    def modStateIn(f: S => S): F[Unit] = self.modState(f)

  //   /** Like `setState` but completes with a `Unit` value *after* the state modification has
  //     * been completed. In contrast, `setState(mod).to[F]` completes with a unit once the state
  //     * modification has been enqueued.
  //     *
  //     * Provides access only to state.
  //     */
  //   def setStateAsyncIn[F[_]: Async](s: S): F[Unit] =
  //     Async[F].async_[Unit] { cb =>
  //       val doMod = self.setState(s, Callback(cb(Right(()))))
  //       doMod
  //         .maybeHandleError { case NonFatal(t) =>
  //           Callback(cb(Left(t)))
  //         }
  //         .runNow()
  //     }
  //
  //   /** Like `modState` but completes with a `Unit` value *after* the state modification has
  //     * been completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
  //     * modification has been enqueued.
  //     *
  //     * Provides access only to state.
  //     */
  //   def modStateAsyncIn[F[_]: Async](mod: S => S): F[Unit] =
  //     Async[F].async_[Unit] { asyncCB =>
  //       val doMod = self.modState(mod, Callback(asyncCB(Right(()))))
  //       doMod
  //         .maybeHandleError { case NonFatal(t) =>
  //           Callback(asyncCB(Left(t)))
  //         }
  //         .runNow()
  //     }
  //
  //   def setStateLIn[F[_]]: SetStateLApplied[F, S] =
  //     new SetStateLApplied[F, S](self)
  //
  //   def modStateLIn[F[_]]: ModStateLApplied[F, S] =
  //     new ModStateLApplied[F, S](self)
  }
  //
  // implicit class ModStateWithPropsFOps[S, P](
  //   private val self: StateAccess.WriteWithProps[CallbackTo, P, S]
  // ) extends AnyVal {
  //
  //   /** Like `modState` but completes with a `Unit` value *after* the state modification has
  //     * been completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
  //     * modification has been enqueued.
  //     *
  //     * Provides access to both state and props.
  //     */
  //   def modStateWithPropsIn[F[_]: Async](
  //     mod: (S, P) => S
  //   ): F[Unit] =
  //     Async[F].async_[Unit] { cb =>
  //       val doMod = self.modState(mod, Callback(cb(Right(()))))
  //       doMod
  //         .maybeHandleError { case NonFatal(t) =>
  //           Callback(cb(Left(t)))
  //         }
  //         .runNow()
  //     }
  // }
  //
  // implicit class EffectAOps[F[_], A](private val self: F[A]) extends AnyVal {
  //
  //   /** Return a `Callback` that will run the effect `F[A]` asynchronously.
  //     *
  //     * @param cb Result handler returning a `F[Unit]`.
  //     */
  //   def runAsyncCB(
  //     cb:         Either[Throwable, A] => F[Unit]
  //   )(implicit F: MonadError[F, Throwable], dispatcher: Dispatcher[F]): Callback =
  //     Callback(dispatcher.unsafeRunAndForget(self.attempt.flatMap(cb)))
  //
  //   /** Return a `Callback` that will run the effect `F[A]` asynchronously.
  //     *
  //     * @param cb Result handler returning a `Callback`.
  //     */
  //   def runAsyncAndThenCB(
  //     cb:         Either[Throwable, A] => Callback
  //   )(implicit F: Sync[F], dispatcher: Dispatcher[F]): Callback =
  //     runAsyncCB(cb.andThen(c => F.delay(c.runNow())))
  //
  //   /** Return a `Callback` that will run the effect `F[A]` asynchronously and discard the result or errors. */
  //   def runAsyncAndForgetCB(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F]
  //   ): Callback =
  //     self.runAsyncCB(_ => F.unit)
  //
  //   def runAsync(
  //     cb:         Either[Throwable, A] => F[Unit]
  //   )(implicit F: MonadError[F, Throwable], dispatcher: Dispatcher[F]): SyncIO[Unit] =
  //     SyncIO(dispatcher.unsafeRunAndForget(self.attempt.flatMap(cb)))
  //
  //   def runAsyncAndThen(
  //     cb:         Either[Throwable, A] => Callback
  //   )(implicit F: Sync[F], dispatcher: Dispatcher[F]): Callback =
  //     runAsync(cb.andThen(c => F.delay(c.runNow())))
  //
  //   def runAsyncAndForget(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F]
  //   ): Callback =
  //     self.runAsync(_ => F.unit)
  // }
  //
  // implicit class EffectUnitOps[F[_]](private val self: F[Unit]) extends AnyVal {
  //
  //   /** Return a `Callback` that will run the effect `F[Unit]` asynchronously and log possible errors.
  //     *
  //     * @param cb `F[Unit]` to run in case of success.
  //     */
  //   def runAsyncAndThenFCB(
  //     cb:         F[Unit],
  //     errorMsg:   String = "Error in F[Unit].runAsyncAndThen"
  //   )(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F],
  //     logger:     Logger[F]
  //   ): Callback =
  //     new EffectAOps(self).runAsyncCB {
  //       case Right(()) => cb
  //       case Left(t)   => logger.error(t)(errorMsg)
  //     }
  //
  //   /** Return a `Callback` that will run the effect `F[Unit]` asynchronously and log possible errors.
  //     *
  //     * @param cb `Callback` to run in case of success.
  //     */
  //   def runAsyncAndThenCB(
  //     cb:         Callback,
  //     errorMsg:   String = "Error in F[Unit].runAsyncAndThenCB"
  //   )(implicit F: Sync[F], dispatcher: Dispatcher[F], logger: Logger[F]): Callback =
  //     runAsyncAndThenFCB(F.delay(cb.runNow()), errorMsg)
  //
  //   /** Return a `Callback` that will run the effect F[Unit] asynchronously and log possible errors. */
  //   def runAsyncCB(
  //     errorMsg:   String = "Error in F[Unit].runAsyncCB"
  //   )(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F],
  //     logger:     Logger[F]
  //   ): Callback =
  //     runAsyncAndThenFCB(F.unit, errorMsg)
  //
  //   def runAsyncCB(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F],
  //     logger:     Logger[F]
  //   ): Callback =
  //     runAsyncCB()
  //
  //   def runAsyncAndThenF(
  //     cb:         F[Unit],
  //     errorMsg:   String = "Error in F[Unit].runAsyncAndThen"
  //   )(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F],
  //     logger:     Logger[F]
  //   ): SyncIO[Unit] =
  //     new EffectAOps(self).runAsync {
  //       case Right(()) => cb
  //       case Left(t)   => logger.error(t)(errorMsg)
  //     }
  //
  //   def runAsyncAndThen(
  //     cb:         SyncIO[Unit],
  //     errorMsg:   String = "Error in F[Unit].runAsyncAndThenCB"
  //   )(implicit F: Sync[F], dispatcher: Dispatcher[F], logger: Logger[F]): SyncIO[Unit] =
  //     runAsyncAndThenF(F.delay(cb.unsafeRunSync()), errorMsg)
  //
  //   def runAsync(
  //     errorMsg:   String = "Error in F[Unit].runAsyncCB"
  //   )(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F],
  //     logger:     Logger[F]
  //   ): SyncIO[Unit] =
  //     runAsyncAndThenF(F.unit, errorMsg)
  //
  //   def runAsync(implicit
  //     F:          MonadError[F, Throwable],
  //     dispatcher: Dispatcher[F],
  //     logger:     Logger[F]
  //   ): SyncIO[Unit] =
  //     runAsync()
  // }
  //
  // // I can't find a generic way to run a `Sync`, so these are only available for SyncIO.
  // implicit class SyncIO2Callback[A](private val s: SyncIO[A]) extends AnyVal {
  //   @inline def toCB: CallbackTo[A] = CallbackTo(s.unsafeRunSync())
  //
  //   @inline def to[F[_]: Async]: F[A] = Async[F].delay(s.unsafeRunSync())
  // }
  //
  // @inline implicit def syncIOToCB[A](s: SyncIO[A]): CallbackTo[A] =
  //   s.toCB
  //
  // @inline implicit def syncIOToUndefOrCB[A](s: SyncIO[A]): js.UndefOr[CallbackTo[A]] =
  //   syncIOToCB(s)
  //
  // @inline implicit def syncIOFnToUndefOrCBFn[A, B](
  //   f: A => SyncIO[B]
  // ): js.UndefOr[A => CallbackTo[B]] =
  //   (a: A) => syncIOToCB(f(a))
  //
  // implicit class PotRender[A](val pot: Pot[A]) extends AnyVal {
  //   def renderPending(f: Long => VdomNode): VdomNode =
  //     pot match {
  //       case Pending(start) => f(start)
  //       case _              => EmptyVdom
  //     }
  //
  //   def renderError(f: Throwable => VdomNode): VdomNode =
  //     pot match {
  //       case Error(t) => f(t)
  //       case _        => EmptyVdom
  //     }
  //
  //   def renderReady(f: A => VdomNode): VdomNode =
  //     pot match {
  //       case Ready(a) => f(a)
  //       case _        => EmptyVdom
  //     }
  // }
  //
  // implicit def throwableReusability: Reusability[Throwable] =
  //   Reusability.byRef[Throwable]
  //
  // implicit def potReusability[A: Reusability](implicit
  //   throwableReusability: Reusability[Throwable]
  // ): Reusability[Pot[A]] =
  //   Reusability((x, y) =>
  //     x match {
  //       case Pending(startx) =>
  //         y match {
  //           case Pending(starty) => startx === starty
  //           case _               => false
  //         }
  //       case Error(tx)       =>
  //         y match {
  //           case Error(ty) => tx ~=~ ty
  //           case _         => false
  //         }
  //       case Ready(ax)       =>
  //         y match {
  //           case Ready(ay) => ax ~=~ ay
  //           case _         => false
  //         }
  //     }
  //   )
  //
  // implicit def viewReusability[F[_], A: Reusability]: Reusability[ViewF[F, A]] =
  //   Reusability.by(_.get)
  //
  // implicit def viewOptReusability[F[_], A: Reusability]: Reusability[ViewOptF[F, A]] =
  //   Reusability.by(_.get)
  //
  // implicit def viewListReusability[F[_], A: Reusability]: Reusability[ViewListF[F, A]] =
  //   Reusability.by(_.get)
  //
  implicit class ViewFModuleOps(private val viewFModule: ViewF.type) extends AnyVal {
    def fromStateSyncIO: FromStateViewSyncIO = new FromStateViewSyncIO
  }

  // implicit class ViewFReuseOps[F[_], G[_], A](private val viewF: ViewOps[F, G, A]) extends AnyVal {
  //   def reuseSet: Reuse[A => F[Unit]] = Reuse.always(viewF.set)
  //
  //   def reuseMod: Reuse[(A => A) => F[Unit]] = Reuse.always(viewF.mod)
  //
  //   def reuseModAndGet(implicit F: Async[F]): Reuse[(A => A) => F[G[A]]] =
  //     Reuse.always(viewF.modAndGet)
  // }
}

package implicits {
  // protected class SetStateLApplied[F[_], S](private val self: StateAccess.Write[CallbackTo, S])
  //     extends AnyVal     {
  //   @inline def apply[A, B](lens: Lens[S, B])(a: A)(implicit conv: A => B, F: Sync[F]): F[Unit] =
  //     self.modStateIn(lens.replace(a))
  // }
  //
  // protected class ModStateLApplied[F[_], S](private val self: StateAccess.Write[CallbackTo, S])
  //     extends AnyVal {
  //   @inline def apply[A](lens: Lens[S, A])(f: A => A)(implicit F: Sync[F]): F[Unit] =
  //     self.modStateIn(lens.modify(f))
  // }
}
