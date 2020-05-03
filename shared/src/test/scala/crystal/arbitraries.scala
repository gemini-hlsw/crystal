package crystal

import org.scalacheck.Arbitrary

object arbitraries {
  implicit def arbCtx[C: Arbitrary, A: Arbitrary]: Arbitrary[Ctx[C, A]] =
    Arbitrary(for {
      c <- Arbitrary.arbitrary[C]
      a <- Arbitrary.arbitrary[A]
    } yield Ctx(a, c))
}
