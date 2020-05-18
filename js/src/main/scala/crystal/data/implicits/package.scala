package crystal.data

import japgolly.scalajs.react.Reusability

package object implicits {
  implicit def throwableReusability: Reusability[Throwable] =
    Reusability.byRef[Throwable]

}
