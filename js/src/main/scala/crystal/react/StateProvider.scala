package crystal.react

import crystal.react.hooks._
import crystal.react.reuse._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.reflect.ClassTag

object StateProvider {
  def apply[M: ClassTag: Reusability](model: M) =
    ScalaFnComponent
      .withHooks[ReuseView[M] ==> VdomNode]
      .useStateViewWithReuse(model)
      .renderWithReuse((f, state) => f(state))
}
