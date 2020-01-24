package crystal

import cats.effect.{IO, SyncIO}
import japgolly.scalajs.react.component.Generic.MountedSimple
import japgolly.scalajs.react.{Callback, CallbackTo, StateAccess}
import scala.scalajs.js
import scala.collection.mutable

import scala.util.control.NonFatal

import scala.language.implicitConversions

package object react {

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

      /*implicit class SyncIoCallbackToOps[A](val self: SyncIO[A]) {
        def toCallback: CallbackTo[A] =
          CallbackTo.lift(() => self.unsafeRunSync)
      }*/

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


      implicit class ModStateIOOps[S, P](
                                          private val self: StateAccess[CallbackTo, S] with StateAccess.ModStateWithProps[CallbackTo, P, S]) {

        /**
         * Like `modState` but completes with a `Unit` value *after* the state modification has
         * been completed. In contrast, `modState(mod).toIO` completes with a unit once the state
         * modification has been enqueued.
         */
        def modStateIO(mod: S => S): IO[Unit] =
          IO.async[Unit] { cb =>
            val doMod = self.modState(mod, Callback.lift(() => cb(Right(()))))
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
            val doMod = self.modState(mod, Callback.lift(() => cb(Right(()))))
            try doMod.runNow()
            catch {
              case NonFatal(t) => cb(Left(t))
            }
          }

        /** Provides access to state `S` in an `IO` */
        def stateIO: IO[S] =
          self.state.toIO
      }

    }

  }

}
