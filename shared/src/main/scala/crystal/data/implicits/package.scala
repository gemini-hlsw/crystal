package crystal.data

import cats.kernel.Eq
import cats.implicits._

package object implicits {
  implicit val eqThrowable: Eq[Throwable] = Eq.by(_.getClass.getName)
}
