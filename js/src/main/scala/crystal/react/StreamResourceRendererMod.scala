package crystal.react

import cats.effect.Sync
import cats.effect.kernel.Resource
import cats.syntax.all._
import crystal.Pot
import crystal._
import crystal.react.StreamRendererMod
import crystal.react.hooks._
import crystal.react.reuse._
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }
import japgolly.scalajs.react.util.Effect.UnsafeSync
import japgolly.scalajs.react.vdom.html_<^._
import org.typelevel.log4cats.Logger
import _root_.react.common.ReactFnProps

import scala.reflect.ClassTag
import scala.concurrent.duration.FiniteDuration

final case class StreamResourceRendererMod[A](
  resource:     Resource[DefaultA, fs2.Stream[DefaultA, A]],
  render:       Pot[ReuseView[A]] ==> VdomNode,
  holdAfterMod: Option[FiniteDuration] = none
)(implicit
  val reuse:    Reusability[A],
  val classTag: ClassTag[A],
  val DefaultS: Sync[DefaultS],
  val dispatch: UnsafeSync[DefaultS],
  val logger:   Logger[DefaultA]
) extends ReactFnProps[StreamResourceRendererMod[Any]](StreamResourceRendererMod.component) {
  def withHoldAfterMod(duration: FiniteDuration): StreamResourceRendererMod[A] =
    copy(holdAfterMod = duration.some)
}

object StreamResourceRendererMod {
  type Props[A] = StreamResourceRendererMod[A]

  private def componentBuilder[A] =
    ScalaFnComponent
      .withHooks[Props[A]]
      .useResourceBy { props =>
        import props.reuse
        import props.classTag
        import props.logger
        import props.DefaultS
        import props.dispatch

        props.resource.map(stream =>
          StreamRendererMod.build[DefaultA, A](stream, props.holdAfterMod)
        )
      }
      .render((props, component) =>
        // The same Pot rendering function is used for rendering the Pot[Component]
        // while the Resource is being allocated/is errored, and for rendering the
        // Pot[A] obtained from the stream.
        component match {
          case p @ Pending(_) => props.render(p)
          case e @ Error(_)   => props.render(e)
          case Ready(comp)    => comp(props.render)
        }
      )

  val component = componentBuilder[Any]
}
