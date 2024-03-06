// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.reuse

import japgolly.scalajs.react.*

protected trait ReusableInteropLowPriority {
  given [A]: Conversion[Reuse[A], Reusable[A]] = r =>
    import r.given
    Reusable.implicitly(r.reuseBy).withLazyValue(r.getValue())

  given [A]: Conversion[Reusable[A], Reuse[A]] = r => Reuse(r).self.map(_.value)
}

/* Convert (A[, B, ...]) ==> Z into A ~=> (B [~=> ...] ~=> Z).
 */
protected trait ReusableInterop extends ReusableInteropLowPriority {
  // Fn1
  class Fn1[R, B](val f: Reuse[R => B]) extends Function1[R, B] {
    def apply(r: R): B = f.value(r)
  }

  given [R, B]: Reusability[Fn1[R, B]] = Reusability.by(_.f)

  given [A, R, B]: Conversion[Reuse[R => B], R ~=> B] = ra => Reusable.implicitly(new Fn1(ra))

  // Fn2
  class Fn2[R: Reusability, S, B](val f: Reuse[(R, S) => B]) extends Function1[R, S ~=> B] {
    def apply(r: R): S ~=> B = f.curry(r)
  }

  given [R, S, B]: Reusability[Fn2[R, S, B]] = Reusability.by(_.f)

  given [A, R: Reusability, S, B]: Conversion[Reuse[((R, S) => B)], R ~=> (S ~=> B)] = ra =>
    Reusable.implicitly(new Fn2(ra))

  // Fn3
  class Fn3[R: Reusability, S: Reusability, T, B](val f: Reuse[(R, S, T) => B])
      extends Function1[R, S ~=> (T ~=> B)] {
    def apply(r: R): S ~=> (T ~=> B) = f.curry(r)
  }

  given reusablityFn3[R, S, T, B]: Reusability[Fn3[R, S, T, B]] = Reusability.by(_.f)

  given [A, R: Reusability, S: Reusability, T, B]
    : Conversion[Reuse[((R, S, T) => B)], R ~=> (S ~=> (T ~=> B))] = ra =>
    Reusable.implicitly(new Fn3(ra))
}
