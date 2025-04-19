// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.Monoid
import cats.effect.Ref
import cats.effect.Sync
import cats.effect.Temporal
import cats.effect.syntax.all.given
import cats.syntax.all.*

import scala.concurrent.duration.FiniteDuration

/**
 * Throttles submitted effects so that they are spaced by at least the specified time. Effects will
 * be discarded if they are submitted while another effect is running, except for the last one.
 */
class Throttler[F[_]: Temporal] private (
  spacedBy:  FiniteDuration,
  isRunning: Ref[F, Boolean], // true if an effect is running
  queued:    Ref[F, Option[F[Unit]]]
)(using Monoid[F[Unit]]):
  def submit(f: F[Unit]): F[Unit] =
    (queued.set(none) >> isRunning.getAndSet(true)).uncancelable
      .flatMap:
        case false =>
          Temporal[F].sleep(spacedBy).both(f) >> // Completes when both complete.
            (isRunning.set(false) >> queued.getAndSet(none)).uncancelable
              .flatMap(_.map(submit).orEmpty)
        case true  =>
          queued.set(f.some)

object Throttler:
  def apply[F[_]: Temporal](spacedBy: FiniteDuration)(using Monoid[F[Unit]]): F[Throttler[F]] =
    for
      isRunning <- Ref.of(false)
      queued    <- Ref.of(none)
    yield new Throttler(spacedBy, isRunning, queued)

  def unsafe[F[_]: Temporal: Sync](spacedBy: FiniteDuration)(using Monoid[F[Unit]]): Throttler[F] =
    new Throttler(spacedBy, Ref.unsafe(false), Ref.unsafe(none))
