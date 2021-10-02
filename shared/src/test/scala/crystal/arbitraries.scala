package crystal

import org.scalacheck._
import Arbitrary._
import Gen._

object arbitraries {
  implicit def arbPot[A: Arbitrary]: Arbitrary[Pot[A]] =
    Arbitrary(
      oneOf(
        arbitrary[Long].map(Pending.apply),
        arbitrary[Throwable].map(Error.apply),
        arbitrary[A].map(Ready.apply)
      )
    )

  implicit def arbPotF[A](implicit fArb: Arbitrary[A => A]): Arbitrary[Pot[A] => Pot[A]] =
    Arbitrary(
      arbitrary[A => A].map(f => _.map(f))
    )
}
