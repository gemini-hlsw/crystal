package crystal

import cats.effect._
import japgolly.scalajs.react.component.Generic.MountedSimple
import japgolly.scalajs.react._

import scala.util.control.NonFatal

package object react {

  import japgolly.scalajs.react.Reusability

  type SetState[F[_], A] = A => F[Unit]
  type ModState[F[_], A] = (A => A) => F[Unit]

  implicit class StreamOps[F[_], A](private val s: fs2.Stream[F, A]) {
    import StreamRenderer._

    def render(implicit ce: ConcurrentEffect[F]): Component[A] =
      StreamRenderer.build(s)
  }

  // The following code adapted from Michael Pilquist's

  object implicits {
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

    implicit class ModStateWithPropsFOps[S, P](
        private val self: StateAccess.WriteWithProps[CallbackTo, P, S]
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

      /**
        * Like `modState` but completes with a `Unit` value *after* the state modification has
        * been completed. In contrast, `modState(mod).to[F]` completes with a unit once the state
        * modification has been enqueued.
        *
        * Provides access to both state and props.
        */
      def modStateIn[F[_]: Async](mod: (S, P) => S): F[Unit] =
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
          cb: Either[Throwable, A] => IO[Unit]
      )(implicit effect: Effect[F]): Callback =
        CallbackTo.lift(() => Effect[F].runAsync(self)(cb).unsafeRunSync())

      def runInCBAndThen(
          cb: A => Callback
      )(implicit effect: Effect[F]): Callback =
        runAsyncInCB {
          case Right(a) => cb(a).to[IO]
          case Left(t)  => IO.raiseError(t)
        }

      def runInCBAndForget()(implicit effect: Effect[F]): Callback =
        self.runAsyncInCB(_ => IO.unit)
    }

    implicit class EffectUnitOps[F[_]](private val self: F[Unit])
        extends AnyVal {
      def runInCBAndThen(
          cb: Callback
      )(implicit effect: Effect[F]): Callback =
        new EffectAOps(self).runInCBAndThen((_: Unit) => cb)

      def runInCB(implicit effect: Effect[F]): Callback =
        self.runInCBAndForget()
    }

    implicit class SyncIO2Callback[A](private val s: SyncIO[A]) extends AnyVal {
      @inline def toCB: CallbackTo[A] = CallbackTo(s.unsafeRunSync())
    }

    implicit def viewCtxReusability[F[_], C, A](
        implicit r: Reusability[A]
    ): Reusability[ViewCtx[F, C, A]] =
      Reusability.by[ViewCtx[F, C, A], A](_.view.get)
  }
}
