// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.syntax

import cats.MonadError
import cats.effect.Async
import cats.effect.Sync
import cats.syntax.all.*
import crystal.*
import crystal.react.hooks.UseSerialState
import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Generic.MountedSimple
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS
import japgolly.scalajs.react.util.Effect
import japgolly.scalajs.react.util.Effect.UnsafeSync
import monocle.Lens
import org.typelevel.log4cats.Logger

import scala.util.control.NonFatal

trait effect {
  extension [A](self: DefaultS[A])
    inline def to[F[_]](using F: Sync[F]): F[A]       =
      F.delay(summon[UnsafeSync[DefaultS]].runSync(self))
    inline def toStream[F[_]: Sync]: fs2.Stream[F, A] =
      fs2.Stream.eval(self.to[F])
    inline def toAsync: DefaultA[A]                   =
      to[DefaultA]
    inline def toAsyncStream: fs2.Stream[DefaultA, A] =
      toStream[DefaultA]

  extension [S, P](self: MountedSimple[DefaultS, DefaultA, P, S])
    def propsIn[F[_]: Sync]: F[P] = self.props.to[F]

  extension [S](self: StateAccess[DefaultS, DefaultA, S])
    /** Provides access to state `S` in an `F` */
    def stateIn[F[_]: Sync]: F[S] = self.state.to[F]

  extension [S](self: StateAccess.Write[DefaultS, DefaultA, S])
    def setStateIn[F[_]: Sync](s: S): F[Unit]      = self.setState(s).to[F]
    def modStateIn[F[_]: Sync](f: S => S): F[Unit] = self.modState(f).to[F]

    /**
     * Like `setState` but completes with a `Unit` value *after* the state modification has been
     * completed. In contrast, `setState(mod).to[F]` completes with a unit once the state
     * modification has been enqueued.
     *
     * Provides access only to state.
     */
    def setStateAsyncIn[F[_]](s: S)(using F: Async[F], dispatch: UnsafeSync[DefaultS]): F[Unit] =
      F.async(cb =>
        self
          .setState(s, DefaultS.delay(cb(Right(()))))
          .maybeHandleError { case NonFatal(t) => DefaultS.delay(cb(Left(t))) }
          .to[F]
          .as(F.unit.some)
      )

    /**
     * Like `modState` but completes with a `Unit` value *after* the state modification has been
     * completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
     * modification has been enqueued.
     *
     * Provides access only to state.
     */
    def modStateAsyncIn[F[_]](
      mod: S => S
    )(using F: Async[F], dispatch: UnsafeSync[DefaultS]): F[Unit] =
      F.async(asyncCB =>
        self
          .modState(mod, DefaultS.delay(asyncCB(Right(()))))
          .maybeHandleError { case NonFatal(t) => DefaultS.delay(asyncCB(Left(t))) }
          .to[F]
          .as(F.unit.some)
      )

    def setStateLIn[F[_]]: SetStateLApplied[F, S] =
      new SetStateLApplied[F, S](self)

    def modStateLIn[F[_]]: ModStateLApplied[F, S] =
      new ModStateLApplied[F, S](self)

  extension [S, P](self: StateAccess.WriteWithProps[DefaultS, DefaultA, P, S])
    /**
     * Like `modState` but completes with a `Unit` value *after* the state modification has been
     * completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
     * modification has been enqueued.
     *
     * Provides access to both state and props.
     */
    def modStateWithPropsIn[F[_]](
      mod: (S, P) => S
    )(using F: Async[F], dispatch: UnsafeSync[DefaultS]): F[Unit] =
      F.async(cb =>
        self
          .modState(mod, DefaultS.delay(cb(Right(()))))
          .maybeHandleError { case NonFatal(t) => DefaultS.delay(cb(Left(t))) }
          .to[F]
          .as(F.unit.some)
      )

  extension [S](self: Hooks.UseState[S])
    inline def setStateAsync: Reusable[S => DefaultA[Unit]] =
      self.setState.map(f => s => f(s).to[DefaultA])

    inline def modStateAsync: Reusable[(S => S) => DefaultA[Unit]] =
      self.modState.map(f => g => f(g).to[DefaultA])

    inline def withReusableInputsAsync: WithReusableInputsAsync[S] =
      new WithReusableInputsAsync[S](self)

  extension [S](self: Hooks.UseStateWithReuse[S])
    inline def setStateAsync: Reusable[S => Reusable[DefaultA[Unit]]] =
      self.setState.map(f => s => f(s).map(_.to[DefaultA]))

    inline def modStateAsync(f: S => S): Reusable[DefaultA[Unit]] =
      self.modState(f).map(_.to[DefaultA])

  extension [A](self: Hooks.UseRef[A])
    inline def setIn[F[_]: Sync](a: A): F[Unit]      = self.set(a).to[F]
    inline def modIn[F[_]: Sync](f: A => A): F[Unit] = self.mod(f).to[F]
    inline def getIn[F[_]: Sync]: F[A]              = self.get.to[F]
    inline def setAsync: A => DefaultA[Unit]        = setIn[DefaultA](_)
    inline def modAsync: (A => A) => DefaultA[Unit] = modIn[DefaultA](_)
    inline def getAsync: DefaultA[A]                = getIn[DefaultA]

  extension [S](self: UseSerialState[S])
    inline def setStateAsync: Reusable[S => DefaultA[Unit]] =
      self.setState.map(f => s => f(s).to[DefaultA])

    inline def modStateAsync: Reusable[(S => S) => DefaultA[Unit]] =
      self.modState.map(f => g => f(g).to[DefaultA])

  extension [F[_], A](self: F[A])
    /**
     * Return a `DefaultS[Unit]` that will run the effect `F[A]` asynchronously.
     *
     * @param cb
     *   Result handler returning a `F[Unit]`.
     */
    def runAsync(
      cb: Either[Throwable, A] => F[Unit]
    )(using F: MonadError[F, Throwable], dispatcher: Effect.Dispatch[F]): DefaultS[Unit] =
      DefaultS.delay(dispatcher.dispatch(self.attempt.flatMap(cb)))

    /**
     * Return a `DefaultS[Unit]` that will run the effect `F[A]` asynchronously.
     *
     * @param cb
     *   Result handler returning a `DefaultS[Unit]`.
     */
    def runAsyncAndThen(
      cb:          Either[Throwable, A] => DefaultS[Unit]
    )(using
      F:           Sync[F],
      dispatcherF: Effect.Dispatch[F],
      dispatchS:   UnsafeSync[DefaultS]
    ): DefaultS[Unit] =
      runAsync(cb.andThen(c => F.delay(dispatchS.runSync(c))))

    /**
     * Return a `DefaultS[Unit]` that will run the effect `F[A]` asynchronously and discard the
     * result or errors.
     */
    def runAsyncAndForget(using
      F:           MonadError[F, Throwable],
      dispatcherF: Effect.Dispatch[F]
    ): DefaultS[Unit] =
      self.runAsync(_ => F.unit)

  extension [F[_]](self: F[Unit])
    /**
     * Return a `DefaultS[Unit]` that will run the effect `F[Unit]` asynchronously and log possible
     * errors.
     *
     * @param cb
     *   `F[Unit]` to run in case of success.
     */
    def runAsyncAndThenF(
      cb:         F[Unit],
      errorMsg:   String = "Error in F[Unit].runAsyncAndThenF"
    )(using
      F:          MonadError[F, Throwable],
      dispatcher: Effect.Dispatch[F],
      logger:     Logger[F]
    ): DefaultS[Unit] =
      // new EffectAOps(self).runAsync {
      self.runAsync {
        case Right(()) => cb
        case Left(t)   => logger.error(t)(errorMsg)
      }

    /**
     * Return a `DefaultS[Unit]` that will run the effect `F[Unit]` asynchronously and log possible
     * errors.
     *
     * @param cb
     *   `DefaultS[Unit]` to run in case of success.
     */
    def runAsyncAndThen(
      cb:          DefaultS[Unit],
      errorMsg:    String = "Error in F[Unit].runAsyncAndThen"
    )(using
      F:           Sync[F],
      dispatcherF: Effect.Dispatch[F],
      logger:      Logger[F],
      dispatchS:   UnsafeSync[DefaultS]
    ): DefaultS[Unit] =
      runAsyncAndThenF(F.delay(dispatchS.runSync(cb)), errorMsg)

    /**
     * Return a `DefaultS[Unit]` that will run the effect F[Unit] asynchronously and log possible
     * errors.
     */
    def runAsync(
      errorMsg:   String = "Error in F[Unit].runAsync"
    )(using
      F:          MonadError[F, Throwable],
      dispatcher: Effect.Dispatch[F],
      logger:     Logger[F]
    ): DefaultS[Unit] =
      runAsyncAndThenF(F.unit, errorMsg)

    def runAsync(using
      F:          MonadError[F, Throwable],
      dispatcher: Effect.Dispatch[F],
      logger:     Logger[F]
    ): DefaultS[Unit] =
      runAsync()

  protected final class SetStateLApplied[F[_], S](
    self: StateAccess.Write[DefaultS, DefaultA, S]
  ) {
    inline def apply[A, B](lens: Lens[S, B])(a: A)(using conv: A => B, F: Sync[F]): F[Unit] =
      self.modStateIn(lens.replace(conv(a)))
  }

  protected final class ModStateLApplied[F[_], S](
    self: StateAccess.Write[DefaultS, DefaultA, S]
  ) {
    inline def apply[A](lens: Lens[S, A])(f: A => A)(using F: Sync[F]): F[Unit] =
      self.modStateIn(lens.modify(f))
  }

  protected final class WithReusableInputsAsync[S](self: Hooks.UseStateF[DefaultS, S]) {
    inline def setState: Reusable[Reusable[S] => Reusable[DefaultA[Unit]]] =
      self.withReusableInputs.setState.map(f => s => f(s).map(_.to[DefaultA]))

    inline def modState: Reusable[Reusable[S => S] => Reusable[DefaultA[Unit]]] =
      self.withReusableInputs.modState.map(f => g => f(g).map(_.to[DefaultA]))
  }
}

object effect extends effect
