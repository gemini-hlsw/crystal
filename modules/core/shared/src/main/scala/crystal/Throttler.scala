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
  spacedBy: FiniteDuration,
  state:    Ref[F, Throttler.State[F]] // running flag and last queued effect, updated atomically
)(using Monoid[F[Unit]]):
  import Throttler.State

  def submit(f: F[Unit]): F[Unit] =
    state
      .modify: // If not running, start now; otherwise queue f.
        case State(false, _) => (State(true, none), startRun(f))
        case State(true, _)  => (State(true, f.some), Monoid[F[Unit]].empty)
      .flatten

  // Runs `f`, spaced by at least `spacedBy`, in the background; then drains the last queued effect.
  private def startRun(f: F[Unit]): F[Unit] =
    (Temporal[F].sleep(spacedBy).both(f) >> // Completes when both complete.
      drain).start.void                     // Execute everything in background.

  // Atomically clears the running flag, re-submitting the last queued effect, if any.
  private val drain: F[Unit] =
    state
      .modify: // If there is a queued effect, start it; otherwise, clear the running flag.
        case State(_, Some(next)) => (State(true, none), startRun(next))
        case State(_, None)       => (State(false, none), Monoid[F[Unit]].empty)
      .flatten

object Throttler:
  // `isRunning` and `queuedEffect` are kept in a single `Ref` so that "is something running?" and "enqueue
  // the last effect" happen as one atomic transition.
  private case class State[F[_]](isRunning: Boolean = false, queuedEffect: Option[F[Unit]] = none)

  def apply[F[_]: Temporal](spacedBy: FiniteDuration)(using Monoid[F[Unit]]): F[Throttler[F]] =
    Ref.of[F, State[F]](State()).map(new Throttler(spacedBy, _))

  def unsafe[F[_]: Temporal: Sync](spacedBy: FiniteDuration)(using Monoid[F[Unit]]): Throttler[F] =
    new Throttler(spacedBy, Ref.unsafe(State()))
