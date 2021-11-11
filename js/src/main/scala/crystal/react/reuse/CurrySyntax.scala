package crystal.react.reuse

import japgolly.scalajs.react.Reusability

import scala.reflect.ClassTag

protected trait CurrySyntax {
  /*
   * Support instantiating the parameter of a R ==> B via the `.curry(...)` method.
   */
  implicit class ReuseFn1Ops[A, R, B](val ra: Reuse[A])(implicit ev: A =:= (R => B)) {
    def curry(
      r:         R
    )(implicit
      classTagR: ClassTag[(ra.B, R)],
      reuseR:    Reusability[R]
    ): Reuse[B] = {
      implicit val rB = ra.reusability
      Reuse.by((ra.reuseBy, r))(ev(ra.value)(r))
    }
  }

  /*
   * Support instantiating some or all of the parameters of a (R, S) ==> B via
   * the `.curry(...)` method.
   */
  implicit class ReuseFn2Ops[A, R, S, B](val ra: Reuse[A])(implicit ev: A =:= ((R, S) => B)) {
    def curry(
      r:         R
    )(implicit
      classTagR: ClassTag[(ra.B, R)],
      reuseR:    Reusability[R]
    ): Reuse[S => B] = {
      implicit val rB = ra.reusability
      Reuse.by((ra.reuseBy, r))(s => ev(ra.value)(r, s))
    }
  }

  // Auto tupling/untupling to support (R, S) ==> B syntax. Since ==> is a type alias,
  // (R, S) is interpreted as a tuple in that case.
  implicit def tupledReuseFn2[A, R, S, B](ra: Reuse[A])(implicit
    ev:                                       A =:= ((R, S) => B)
  ): (R, S) ==> B =
    ra.map(f => ev(f).tupled)

  implicit def untupledReuseFn2[A, R, S, B](ra: Reuse[A])(implicit
    ev:                                         A =:= (((R, S)) => B)
  ): Reuse[(R, S) => B] =
    ra.map(f => (r, s) => ev(f)((r, s)))

  implicit class ReuseFn2TupledOps[A, R, S, B](val ra: Reuse[A])(implicit
    ev:                                                A =:= (((R, S)) => B)
  ) {
    def curry(r: R)(implicit reuseR: Reusability[R]): Reuse[S => B] =
      (ra: Reuse[(R, S) => B]).curry(r)
  }

  /*
   * Support instantiating some or all of the parameters of a (R, S, T) ==> B via
   * the `.curry(...)` method.
   */
  implicit class ReuseFn3Ops[A, R, S, T, B](val ra: Reuse[A])(implicit ev: A =:= ((R, S, T) => B)) {
    def curry(
      r:         R
    )(implicit
      classTagR: ClassTag[(ra.B, R)],
      reuseR:    Reusability[R]
    ): Reuse[(S, T) => B] = {
      implicit val rB = ra.reusability
      Reuse.by((ra.reuseBy, r))((s, t) => ev(ra.value)(r, s, t))
    }
  }

  // Auto tupling/untupling to support (R, S, T) ==> B syntax. Since ==> is a type alias,
  // (R, S, T) is interpreted as a tuple in that case.
  implicit def tupledReuseFn3[A, R, S, T, B](ra: Reuse[A])(implicit
    ev:                                          A =:= ((R, S, T) => B)
  ): (R, S, T) ==> B =
    ra.map(f => ev(f).tupled)

  implicit def untupledReuseFn3[A, R, S, T, B](ra: Reuse[A])(implicit
    ev:                                            A =:= (((R, S, T)) => B)
  ): Reuse[(R, S, T) => B] =
    ra.map(f => (r, s, t) => ev(f)((r, s, t)))

  implicit class ReuseFn3TupledOps[A, R, S, T, B](val ra: Reuse[A])(implicit
    ev:                                                   A =:= (((R, S, T)) => B)
  ) {
    def curry(r: R)(implicit reuseR: Reusability[R]): Reuse[(S, T) => B] =
      (ra: Reuse[(R, S, T) => B]).curry(r)
  }

}
