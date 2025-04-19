// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.Applicative
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

// Typeclass for normalizing effects into an effect that returns a cleanup effect.
opaque type EffectWithCleanup[G, F[_]] = G => F[F[Unit]]

object EffectWithCleanup:
  // Effect already returns a cleanup effect, return as is.
  given effectWithNoCleanup: EffectWithCleanup[DefaultA[DefaultA[Unit]], DefaultA] =
    identity

  // Effect doesn't return a cleanup effect, add a no-op cleanup.
  given effectWithCleanup: EffectWithCleanup[DefaultA[Unit], DefaultA] =
    _.as(Applicative[DefaultA].unit)

extension [G, F[_]](effect: G)(using effectWithCleanup: EffectWithCleanup[G, F])
  def normalize: F[F[Unit]] = effectWithCleanup(effect)
