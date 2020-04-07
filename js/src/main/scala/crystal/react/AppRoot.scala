package crystal.react

import cats.effect.ConcurrentEffect
import cats.effect.Timer
import japgolly.scalajs.react.component.Generic.UnmountedWithRoot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import crystal.ViewCtx

import scala.language.higherKinds

object AppRoot {
  type AppRootProps[F[_], C, M] = ViewCtx[F, C, M] => VdomNode
  type ReactAppRootComponent[F[_], C, M] = 
    CtorType.Props[AppRootProps[F, C, M], UnmountedWithRoot[AppRootProps[F, C, M], _, _, _]]

  def component[F[_]] = new Apply[F]

  class Apply[F[_]] {
    def apply[M, C](model: M, ctx: C)(implicit ce: ConcurrentEffect[F], timer: Timer[F]) = {
      // TODO We don't really need the whole Stream machinery for this. Make a simpler component
      // that just receives an initial state.
      val Renderer = crystal.react.StreamRendererMod.build[F, M](fs2.Stream.emit(model).covary[F])

      ScalaComponent.builder[ViewCtx[F, C, M] => VdomNode]("AppRoot")
        .render_P{ props =>
          Renderer(m => props(ViewCtx(m, ctx)))
        }
        .build
    }    
  }
}