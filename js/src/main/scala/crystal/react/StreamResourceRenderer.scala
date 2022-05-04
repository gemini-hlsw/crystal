package crystal.react

import cats.effect.kernel.Resource
import crystal.Pot
import crystal._
import crystal.react.StreamRenderer
import crystal.react.hooks._
import crystal.react.reuse._
import japgolly.scalajs.react._
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }
import japgolly.scalajs.react.vdom.html_<^._
import org.typelevel.log4cats.Logger
import _root_.react.common.ReactFnProps

final case class StreamResourceRenderer[A](
  resource:           Resource[DefaultA, fs2.Stream[DefaultA, A]],
  render:             Pot[A] ==> VdomNode
)(implicit val reuse: Reusability[A], val logger: Logger[DefaultA])
    extends ReactFnProps[StreamResourceRenderer[Any]](StreamResourceRenderer.component)

object StreamResourceRenderer {
  type Props[A] = StreamResourceRenderer[A]

  private def componentBuilder[A] =
    ScalaFnComponent
      .withHooks[Props[A]]
      .useResourceBy { props =>
        import props.reuse
        import props.logger

        props.resource.map(StreamRenderer.build[DefaultA, A])
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
