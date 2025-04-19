// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

/**
 * Result from `ViewThrottlerF`.
 */
case class ThrottlingViewF[F[_], G[_], A](
  throttlerView: ViewF[F, A],
  throttledView: ViewF[G, A]
)
