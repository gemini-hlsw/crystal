package crystal

import japgolly.scalajs.react.vdom.VdomNode

package object data {
  implicit class PotRender[A](val pot: Pot[A]) extends AnyVal {
    def render(rp: Long => VdomNode, re: Throwable => VdomNode, rr: A => VdomNode): VdomNode =
      pot.fold(start => rp(System.currentTimeMillis() - start), re, rr)
  }

}
