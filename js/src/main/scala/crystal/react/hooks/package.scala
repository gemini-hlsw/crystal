package crystal.react

import cats.effect.Fiber

package object hooks extends UseDebouncedTimeout.HooksApiExt {
  type TimeoutHandleLatch[F[_]] = Fiber[F, Throwable, Unit]
}
