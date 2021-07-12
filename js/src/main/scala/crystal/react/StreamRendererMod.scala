package crystal.react

import crystal._
import crystal.react.implicits._
import cats.syntax.all._
import cats.effect._
import cats.effect.std.Dispatcher
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.{ Ref => _, _ }
import japgolly.scalajs.react.vdom.html_<^._
import org.typelevel.log4cats.Logger
import crystal.react.reuse._

import scala.concurrent.duration.FiniteDuration

object StreamRendererMod {
  //
  // type Props[A] = Pot[ViewF[SyncIO, A]] ==> VdomNode
  //
  // type State[A]     = Pot[A]
  // type Component[A] =
  //   CtorType.Props[Props[A], UnmountedWithRoot[
  //     Props[A],
  //     _,
  //     _,
  //     _
  //   ]]
  //
  // def build[F[_]: Async: Dispatcher: Logger, A](
  //   stream:       fs2.Stream[F, A],
  //   holdAfterMod: Option[FiniteDuration] = None
  // )(implicit
  //   reuse:        Reusability[A] /* Used to derive Reusability[State[A]] */
  // ): Component[A] = {
  //   class Backend($ : BackendScope[Props[A], State[A]])
  //       extends StreamRendererBackend[F, A](stream) {
  //
  //     val hold: Hold[F, Pot[A]] =
  //       Hold($.setStateIn[F], holdAfterMod)
  //         .unsafeRunSync() // We cannot initialize the Backend effectfully, so we do this.
  //
  //     override protected val directSetState: Pot[A] => F[Unit] = $.setStateIn[F]
  //
  //     override protected lazy val streamSetState: Pot[A] => F[Unit] =
  //       pot =>
  //         hold.set(pot) >> hold.enable // This will debounce messages for at least the hold time.
  //
  //     def render(
  //       props: Props[A],
  //       state: Pot[A]
  //     ): VdomNode =
  //       props(
  //         state.map(a =>
  //           ViewF[SyncIO, A](
  //             a,
  //             (f: A => A, cb: A => SyncIO[Unit]) =>
  //               hold.enable.runAsync.flatMap(_ =>
  //                 // I'm not very happy about the cast.
  //                 // However, this is only executed when the pot is ready, so the cast should be safe.
  //                 $.modStateInSyncIO(_.map(f), pot => cb(pot.asInstanceOf[Ready[A]].value))
  //               )
  //           )
  //         )
  //       )
  //   }
  //
  //   ScalaComponent
  //     .builder[Props[A]]
  //     .initialState(Pot.pending[A])
  //     .renderBackend[Backend]
  //     .componentDidMount(_.backend.startUpdates)
  //     .componentWillUnmount(_.backend.stopUpdates)
  //     .configure(Reusability.shouldComponentUpdate)
  //     .build
  // }
}
