package crystal.react

import cats.Monad
import cats.MonadError
import cats.effect.Async
import cats.effect.Sync
import cats.syntax.all._
import crystal._
import crystal.react.hooks.UseSerialState
import crystal.react.reuse._
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Generic.MountedSimple
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }
import japgolly.scalajs.react.util.Effect
import japgolly.scalajs.react.util.Effect.UnsafeSync
import japgolly.scalajs.react.vdom.html_<^._
import monocle.Lens
import org.typelevel.log4cats.Logger

import scala.reflect.ClassTag
import scala.util.control.NonFatal

package object implicits {
  implicit class DefaultSToOps[A](private val self: DefaultS[A])(implicit
    dispatch:                                       UnsafeSync[DefaultS]
  ) {
    @inline def to[F[_]: Sync]: F[A] = Sync[F].delay(dispatch.runSync(self))

    @inline def toStream[F[_]: Sync]: fs2.Stream[F, A] =
      fs2.Stream.eval(self.to[F])
  }

  implicit class ModMountedSimpleFOps[S, P](
    private val self: MountedSimple[DefaultS, DefaultA, P, S]
  ) extends AnyVal {
    def propsIn[F[_]: Sync]: F[P] = self.props.to[F]
  }

  implicit class StateAccessorFOps[S](
    private val self: StateAccess[DefaultS, DefaultA, S]
  ) extends AnyVal {

    /** Provides access to state `S` in an `F` */
    def stateIn[F[_]: Sync]: F[S] = self.state.to[F]
  }

  implicit class ModStateFOps[S](
    private val self: StateAccess.Write[DefaultS, DefaultA, S]
  ) extends AnyVal {

    def setStateIn[F[_]: Sync](s: S): F[Unit]      = self.setState(s).to[F]
    def modStateIn[F[_]: Sync](f: S => S): F[Unit] = self.modState(f).to[F]

    /** Like `setState` but completes with a `Unit` value *after* the state modification has been
      * completed. In contrast, `setState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access only to state.
      */
    def setStateAsyncIn[F[_]: Async](s: S)(implicit dispatch: UnsafeSync[DefaultS]): F[Unit] =
      Async[F].async_[Unit] { cb =>
        val doMod = self.setState(s, DefaultS.delay(cb(Right(()))))
        dispatch.runSync(
          doMod
            .maybeHandleError { case NonFatal(t) =>
              DefaultS.delay(cb(Left(t)))
            }
        )
      }

    /** Like `modState` but completes with a `Unit` value *after* the state modification has been
      * completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access only to state.
      */
    def modStateAsyncIn[F[_]: Async](
      mod:               S => S
    )(implicit dispatch: UnsafeSync[DefaultS]): F[Unit] =
      Async[F].async_[Unit] { asyncCB =>
        val doMod = self.modState(mod, DefaultS.delay(asyncCB(Right(()))))
        dispatch.runSync(
          doMod
            .maybeHandleError { case NonFatal(t) =>
              DefaultS.delay(asyncCB(Left(t)))
            }
        )
      }

    def setStateLIn[F[_]]: SetStateLApplied[F, S] =
      new SetStateLApplied[F, S](self)

    def modStateLIn[F[_]]: ModStateLApplied[F, S] =
      new ModStateLApplied[F, S](self)
  }

  implicit class ModStateWithPropsFOps[S, P](
    private val self: StateAccess.WriteWithProps[DefaultS, DefaultA, P, S]
  ) extends AnyVal {

    /** Like `modState` but completes with a `Unit` value *after* the state modification has been
      * completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
      * modification has been enqueued.
      *
      * Provides access to both state and props.
      */
    def modStateWithPropsIn[F[_]: Async](
      mod:               (S, P) => S
    )(implicit dispatch: UnsafeSync[DefaultS]): F[Unit] =
      Async[F].async_[Unit] { cb =>
        val doMod = self.modState(mod, DefaultS.delay(cb(Right(()))))
        dispatch.runSync(
          doMod
            .maybeHandleError { case NonFatal(t) =>
              DefaultS.delay(cb(Left(t)))
            }
        )
      }
  }

  implicit class UseStateOps[S](private val self: Hooks.UseState[S]) extends AnyVal {
    @inline def setStateAsync: Reusable[S => DefaultA[Unit]] =
      self.setState.map(f => s => f(s).to[DefaultA])

    @inline def modStateAsync: Reusable[(S => S) => DefaultA[Unit]] =
      self.modState.map(f => g => f(g).to[DefaultA])

    @inline def withReusableInputsAsync: WithReusableInputsAsync[S] =
      new WithReusableInputsAsync[S](self)
  }

  implicit class UseStateWithReuseOps[S](private val self: Hooks.UseStateWithReuse[S])
      extends AnyVal {
    @inline def setStateAsync: Reusable[S => Reusable[DefaultA[Unit]]] =
      self.setState.map(f => s => f(s).map(_.to[DefaultA]))

    @inline def modStateAsync(f: S => S): Reusable[DefaultA[Unit]] =
      self.modState(f).map(_.to[DefaultA])
  }

  implicit class UseRefOps[A](private val self: Hooks.UseRef[A]) extends AnyVal {
    @inline def setAsync: A => DefaultA[Unit] = a => self.set(a).to[DefaultA]

    @inline def modAsync: (A => A) => DefaultA[Unit] = f => self.mod(f).to[DefaultA]

    @inline def getAsync: DefaultA[A] = self.get.to[DefaultA]
  }

  implicit class UseSerialStateOps[S](private val self: UseSerialState[S]) extends AnyVal {
    @inline def setStateAsync: Reusable[S => DefaultA[Unit]] =
      self.setState.map(f => s => f(s).to[DefaultA])

    @inline def modStateAsync: Reusable[(S => S) => DefaultA[Unit]] =
      self.modState.map(f => g => f(g).to[DefaultA])
  }

  implicit class EffectAOps[F[_], A](private val self: F[A]) extends AnyVal {

    /** Return a `DefaultS[Unit]` that will run the effect `F[A]` asynchronously.
      *
      * @param cb
      *   Result handler returning a `F[Unit]`.
      */
    def runAsync(
      cb:         Either[Throwable, A] => F[Unit]
    )(implicit F: MonadError[F, Throwable], dispatcher: Effect.Dispatch[F]): DefaultS[Unit] =
      DefaultS.delay(dispatcher.dispatch(self.attempt.flatMap(cb)))

    /** Return a `DefaultS[Unit]` that will run the effect `F[A]` asynchronously.
      *
      * @param cb
      *   Result handler returning a `DefaultS[Unit]`.
      */
    def runAsyncAndThen(
      cb:          Either[Throwable, A] => DefaultS[Unit]
    )(implicit
      F:           Sync[F],
      dispatcherF: Effect.Dispatch[F],
      dispatchS:   UnsafeSync[DefaultS]
    ): DefaultS[Unit] =
      runAsync(cb.andThen(c => F.delay(dispatchS.runSync(c))))

    /** Return a `DefaultS[Unit]` that will run the effect `F[A]` asynchronously and discard the
      * result or errors.
      */
    def runAsyncAndForget(implicit
      F:           MonadError[F, Throwable],
      dispatcherF: Effect.Dispatch[F]
    ): DefaultS[Unit] =
      self.runAsync(_ => F.unit)
  }

  implicit class EffectUnitOps[F[_]](private val self: F[Unit]) extends AnyVal {

    /** Return a `DefaultS[Unit]` that will run the effect `F[Unit]` asynchronously and log possible
      * errors.
      *
      * @param cb
      *   `F[Unit]` to run in case of success.
      */
    def runAsyncAndThenF(
      cb:         F[Unit],
      errorMsg:   String = "Error in F[Unit].runAsyncAndThenF"
    )(implicit
      F:          MonadError[F, Throwable],
      dispatcher: Effect.Dispatch[F],
      logger:     Logger[F]
    ): DefaultS[Unit] =
      new EffectAOps(self).runAsync {
        case Right(()) => cb
        case Left(t)   => logger.error(t)(errorMsg)
      }

    /** Return a `DefaultS[Unit]` that will run the effect `F[Unit]` asynchronously and log possible
      * errors.
      *
      * @param cb
      *   `DefaultS[Unit]` to run in case of success.
      */
    def runAsyncAndThen(
      cb:          DefaultS[Unit],
      errorMsg:    String = "Error in F[Unit].runAsyncAndThen"
    )(implicit
      F:           Sync[F],
      dispatcherF: Effect.Dispatch[F],
      logger:      Logger[F],
      dispatchS:   UnsafeSync[DefaultS]
    ): DefaultS[Unit] =
      runAsyncAndThenF(F.delay(dispatchS.runSync(cb)), errorMsg)

    /** Return a `DefaultS[Unit]` that will run the effect F[Unit] asynchronously and log possible
      * errors.
      */
    def runAsync(
      errorMsg:   String = "Error in F[Unit].runAsync"
    )(implicit
      F:          MonadError[F, Throwable],
      dispatcher: Effect.Dispatch[F],
      logger:     Logger[F]
    ): DefaultS[Unit] =
      runAsyncAndThenF(F.unit, errorMsg)

    def runAsync(implicit
      F:          MonadError[F, Throwable],
      dispatcher: Effect.Dispatch[F],
      logger:     Logger[F]
    ): DefaultS[Unit] =
      runAsync()
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

  implicit class ViewDefaultSOps[A](private val view: View[A]) {
    def async(implicit logger: Logger[DefaultA]): ViewF[DefaultA, A] =
      view.to[DefaultA](syncToAsync.apply[Unit] _, _.runAsync)
  }

  implicit class ViewFOps[F[_], A: ClassTag: Reusability](private val view: ViewF[F, A]) {
    def reuseByValue: Reuse[ViewF[F, A]] = Reuse.by(view.get)(view)
  }

  implicit class ViewFModuleOps(private val viewFModule: ViewF.type) extends AnyVal {
    def fromState: FromStateView = new FromStateView
  }

  implicit class ViewOptFOps[F[_], A: ClassTag: Reusability](private val view: ViewOptF[F, A]) {
    def reuseByValue: Reuse[ViewOptF[F, A]] = Reuse.by(view.get)(view)
  }

  implicit class OptViewFOps[F[_]: Monad, A](private val optView: Option[ViewF[F, A]]) {
    def toViewOpt: ViewOptF[F, A] =
      optView.fold(new ViewOptF[F, A](none, (_, cb) => cb(none)) {
        override def modAndGet(f: A => A)(implicit F: Async[F]): F[Option[A]] = none.pure[F]
      })(_.asViewOpt)
  }

  implicit class ReuseViewDefaultSOps[A](private val view: ReuseView[A]) {
    def async(implicit logger: Logger[DefaultA]): ReuseViewF[DefaultA, A] =
      view.to[DefaultA](syncToAsync.apply[Unit] _, _.runAsync)
  }

}

package implicits {
  protected final class SetStateLApplied[F[_], S](
    private val self: StateAccess.Write[DefaultS, DefaultA, S]
  ) extends AnyVal {
    @inline def apply[A, B](lens: Lens[S, B])(a: A)(implicit conv: A => B, F: Sync[F]): F[Unit] =
      self.modStateIn(lens.replace(conv(a)))
  }

  protected final class ModStateLApplied[F[_], S](
    private val self: StateAccess.Write[DefaultS, DefaultA, S]
  ) extends AnyVal {
    @inline def apply[A](lens: Lens[S, A])(f: A => A)(implicit F: Sync[F]): F[Unit] =
      self.modStateIn(lens.modify(f))
  }

  protected final class WithReusableInputsAsync[S](self: Hooks.UseStateF[DefaultS, S]) {
    @inline def setState: Reusable[Reusable[S] => Reusable[DefaultA[Unit]]] =
      self.withReusableInputs.setState.map(f => s => f(s).map(_.to[DefaultA]))

    @inline def modState: Reusable[Reusable[S => S] => Reusable[DefaultA[Unit]]] =
      self.withReusableInputs.modState.map(f => g => f(g).map(_.to[DefaultA]))
  }

}
