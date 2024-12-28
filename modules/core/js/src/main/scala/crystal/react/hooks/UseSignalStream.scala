// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.Eq
import cats.effect.std.Queue
import cats.syntax.all.*
import crystal.Pot
import crystal.react.*
import crystal.react.syntax.pot.given
import fs2.Pipe
import japgolly.scalajs.react.*
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

private final def useSignalStreamBuilder[A](
  value: A
)(coalesce: Pipe[DefaultA, A, A]): HookResult[Reusable[Pot[fs2.Stream[DefaultA, A]]]] =
  for
    queue  <- useEffectResultOnMount:
                Queue.unbounded[DefaultA, Option[A]].flatTap(_.offer(value.some))
    _      <- useEffect(queue.toOption.map(_.offer(value.some)).orEmpty)
    stream <- useMemo(queue.void): _ =>
                queue.map:
                  fs2.Stream.fromQueueNoneTerminated(_).through(coalesce)
    _      <- useEffectWhenDepsReady(queue): q => // terminate stream on unmount
                CallbackTo(q.offer(none))
  yield stream

inline final def useSignalStream[A: Eq](
  value: A
): HookResult[Reusable[Pot[fs2.Stream[DefaultA, A]]]] =
  useSignalStreamBuilder(value)(_.changes)

inline def useSignalStreamByReuse[A: Reusability](
  value: A
): HookResult[Reusable[Pot[fs2.Stream[DefaultA, A]]]] =
  useSignalStreamBuilder(value)(_.filterWithPrevious(summon[Reusability[A]].updateNeeded))
