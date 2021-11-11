package crystal.react

import cats.syntax.all._
import crystal.ViewF
import japgolly.scalajs.react._
import japgolly.scalajs.react.feature.Context
import japgolly.scalajs.react.vdom.html_<^._

class Ctx[F[_], C] {
  private val ctx: Context[Option[ViewF[F, C]]] = React.createContext(none)

  def provide(c: ViewF[F, C]): Context.Provided[Option[ViewF[F, C]]] = ctx.provide(c.some)

  def using[A](f: C => A)(implicit ev: A => VdomNode): VdomElement =
    ctx.consume(_.fold[VdomNode](<.div)(c => ev(f(c.get))))

  def usingView[A](f: ViewF[F, C] => A)(implicit ev: A => VdomNode): VdomElement =
    ctx.consume(_.fold[VdomNode](<.div)(c => ev(f(c))))
}
