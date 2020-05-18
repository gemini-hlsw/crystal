package crystal.data

import cats.kernel.Eq

package object implicits {

  // Copied from cats-effect-laws utils.
  implicit val eqThrowable: Eq[Throwable] =
    new Eq[Throwable] {
      def eqv(x: Throwable, y: Throwable): Boolean =
        // All exceptions are non-terminating and given exceptions
        // aren't values (being mutable, they implement reference
        // equality), then we can't really test them reliably,
        // especially due to race conditions or outside logic
        // that wraps them (e.g. ExecutionException)
        (x ne null) == (y ne null)
    }
}
