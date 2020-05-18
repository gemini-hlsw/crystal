package crystal.data.react

import japgolly.scalajs.react.Reusability

package object implicits {
  implicit def throwableReusability: Reusability[Throwable] =
    Reusability.byRef[Throwable]

}
