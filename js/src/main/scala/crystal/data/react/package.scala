package crystal.data

import cats.implicits._
import crystal.data.Pot._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._

package object react {
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
}
