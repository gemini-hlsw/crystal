// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.Eq
import cats.Endo
import cats.syntax.all.*
import crystal.Pot
import crystal.react.*
import crystal.react.syntax.pot.given
import fs2.concurrent.SignallingRef
import japgolly.scalajs.react.*
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

private final def useSignalStreamBuilder[A](
  value: A
)(coalesce: Endo[fs2.Stream[DefaultA, A]]): HookResult[Reusable[Pot[fs2.Stream[DefaultA, A]]]] =
  for
    signallingRef <- useEffectResultOnMount(SignallingRef[DefaultA, A](value))
    _             <- useEffect(signallingRef.toOption.map(_.set(value)).orEmpty)
    stream        <- useMemo(signallingRef.void): _ =>
                       signallingRef.map(sr => coalesce(sr.discrete))
  yield stream

inline final def useSignalStream[A: Eq](
  value: A
): HookResult[Reusable[Pot[fs2.Stream[DefaultA, A]]]] =
  useSignalStreamBuilder(value)(_.changes)

inline def useSignalStreamByReuse[A: Reusability](
  value: A
): HookResult[Reusable[Pot[fs2.Stream[DefaultA, A]]]] =
  useSignalStreamBuilder(value)(_.filterWithPrevious(summon[Reusability[A]].updateNeeded))
