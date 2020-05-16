package crystal

import cats.implicits._
import crystal.data.Pot._
import crystal.data.implicits._
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react._

package object data {
  implicit class PotRender[A](val pot: Pot[A]) extends AnyVal {
    def render(rp: Long => VdomNode, re: Throwable => VdomNode, rr: A => VdomNode): VdomNode =
      pot.fold(start => rp(System.currentTimeMillis() - start), re, rr)
  }

  implicit def potReuse[A: Reusability]: Reusability[Pot[A]] =
    Reusability((x, y) =>
      x match {
        case Pending(startx) =>
          y match {
            case Pending(starty) => startx === starty
            case _               => false
          }
        case Error(tx)       =>
          y match {
            case Error(ty) => tx === ty
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
