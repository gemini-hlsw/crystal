// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.Throttler
import japgolly.scalajs.react.*
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

import scala.concurrent.duration.FiniteDuration

/**
 * Returns a memoized callback that won't be run more than once every `spacedBy` duration, even if
 * invoked more often. If multiple invocations occur within the `spacedBy` duration, only the last
 * one will be run.
 */
def useThrottledCallbackWithDeps[D: Reusability](
  deps: => D
)(spacedBy: FiniteDuration)(callback: D => DefaultA[Unit]): HookResult[Reusable[DefaultA[Unit]]] =
  for
    throttler <- useMemo(())(_ => Throttler.unsafe[DefaultA](spacedBy))
    cb        <- useCallbackWithDeps(deps)(d => throttler.submit(callback(d)))
  yield cb

/**
 * Returns a memoized callback that won't be run more than once every `spacedBy` duration, even if
 * invoked more often. If multiple invocations occur within the `spacedBy` duration, only the last
 * one will be run.
 */
inline def useThrottledCallback(
  spacedBy: FiniteDuration
)(effect: DefaultA[Unit]): HookResult[Reusable[DefaultA[Unit]]] =
  useThrottledCallbackWithDeps(())(spacedBy)(_ => effect)
