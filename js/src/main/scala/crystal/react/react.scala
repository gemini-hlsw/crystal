package crystal

import cats.effect.{ConcurrentEffect, IO, SyncIO}
import japgolly.scalajs.react.component.Generic.MountedSimple
import japgolly.scalajs.react.{Callback, CallbackTo, StateAccess}
import scala.scalajs.js
import scala.collection.mutable

import scala.util.control.NonFatal

import scala.language.higherKinds
import scala.language.implicitConversions

package object react {

  implicit class StreamOps[F[_] : ConcurrentEffect, A](s: fs2.Stream[F, A]) {
    import StreamRenderer._

    def render: ReactStreamRendererComponent[A] = 
      StreamRenderer.build(s)
  }

  object io {

    object implicits {
      @inline implicit def syncIO2Callback[A](s: SyncIO[A]): Callback = Callback {
        s.unsafeRunSync()
      }

      @inline implicit def io2Callback[A](io: IO[A]): Callback = Callback {
        io.unsafeRunAsyncAndForget()
      }

      @inline implicit def io2UndefOrCallback[A](io: IO[A]): js.UndefOr[Callback] = 
        io2Callback(io)

      // All following code thanks to Michael Pilquist

      implicit class IoUnitOps(val self: IO[Unit]) {
        def startAsCallback(errorHandler: Throwable => Callback): Callback =
          CallbackTo.lift(() =>
            self.unsafeRunAsync {
              case Left(e) => errorHandler(e).runNow()
              case Right(_) => ()
            })
      }

      implicit class CallbackToOps[A](val self: CallbackTo[A]) {
        def toIO: IO[A] = IO(self.runNow())
      }

      implicit class ModMountedSimpleIOOps[S, P](
                                                  private val self: MountedSimple[CallbackTo, P, S]) {

        def propsIO: IO[P] = self.props.toIO
      }

      implicit class StateAccessorIOOps[S](private val self: StateAccess[CallbackTo, S]) {

        /** Provides access to state `S` in an `IO` */
        def stateIO: IO[S] =
          self.state.toIO
      }

      implicit class ModStateWithPropsIOOps[S, P](
        private val self: StateAccess.WriteWithProps[CallbackTo, P, S]
      ) {
        def setStateIO(s: S): IO[Unit] =
          IO.async[Unit] { cb =>
            val doMod = self.setState(s, Callback(cb(Right(()))))
            try doMod.runNow()
            catch {
              case NonFatal(t) => cb(Left(t))
            }
          }

        /**
          * Like `modState` but completes with a `Unit` value *after* the state modification has
          * been completed. In contrast, `modState(mod).toIO` completes with a unit once the state
          * modification has been enqueued.
          *
          * Provides access to both state and props.
          */
        def modStateIO(mod: S => S): IO[Unit] =
          IO.async[Unit] { cb =>
            val doMod = self.modState(mod, Callback(cb(Right(()))))
            try doMod.runNow()
            catch {
              case NonFatal(t) => cb(Left(t))
            }
          }

        /**
          * Like `modState` but completes with a `Unit` value *after* the state modification has
          * been completed. In contrast, `modState(mod).toIO` completes with a unit once the state
          * modification has been enqueued.
          *
          * Provides access to both state and props.
          */
        def modStateIO(mod: (S, P) => S): IO[Unit] =
          IO.async[Unit] { cb =>
            val doMod = self.modState(mod, Callback(cb(Right(()))))
            try doMod.runNow()
            catch {
              case NonFatal(t) => cb(Left(t))
            }
          }
      }
    }
  }
}
