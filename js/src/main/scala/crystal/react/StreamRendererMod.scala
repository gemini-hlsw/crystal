package crystal.react

import cats.effect.Async
import cats.effect.Sync
import cats.syntax.all._
import crystal._
import crystal.react.implicits._
import crystal.react.reuse._
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }
import japgolly.scalajs.react.util.Effect
import japgolly.scalajs.react.util.Effect.UnsafeSync
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{ Ref => _, _ }
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object StreamRendererMod {

  type Props[A] = Pot[ReuseView[A]] ==> VdomNode

  type State[A]     = Pot[A]
  type Component[A] =
    CtorType.Props[Props[A], UnmountedWithRoot[
      Props[A],
      _,
      _,
      _
    ]]

  def build[F[_]: Async: Effect.Dispatch: Logger, A: ClassTag: Reusability](
    stream:       fs2.Stream[F, A],
    holdAfterMod: Option[FiniteDuration] = none
  )(implicit
    DefaultS:     Sync[DefaultS],
    dispatch:     UnsafeSync[DefaultS]
  ): Component[A] = {
    class Backend($ : BackendScope[Props[A], State[A]])
        extends StreamRendererBackend[F, A](stream) {

      val hold: Hold[F, Pot[A]] =
        // We cannot initialize the Backend effectfully, so we do this.
        dispatch.runSync(Hold($.setStateIn[F], holdAfterMod))

      override protected val directSetState: Pot[A] => F[Unit] = $.setStateIn[F]

      override protected lazy val streamSetState: Pot[A] => F[Unit] =
        pot =>
          hold.set(pot) >> hold.enable // This will debounce messages for at least the hold time.

      def render(props: Props[A], state: Pot[A]): VdomNode =
        props(
          state.map(a =>
            View[A](
              a,
              (f: A => A, cb: A => DefaultS[Unit]) =>
                hold.enable.runAsync.flatMap(_ =>
                  // I'm not very happy about the cast.
                  // However, this is only executed when the pot is ready, so the cast should be safe.
                  $.modState(_.map(f), $.state.flatMap(pot => cb(pot.asInstanceOf[Ready[A]].value)))
                )
            ).reuseByValue
          )
        )
    }

    ScalaComponent
      .builder[Props[A]]
      .initialState(Pot.pending[A])
      .renderBackend[Backend]
      .componentDidMount(_.backend.startUpdates)
      .componentWillUnmount(_.backend.stopUpdates)
      .configure(Reusability.shouldComponentUpdate)
      .build
  }
}
