// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

import scala.concurrent.duration.*

// Exercises `useAsyncEffect` against its README description: "Version of `useEffect` that avoids
// race conditions when executing async effects [...] by cancelling the previous instance of the
// effect before executing a new one."
class UseAsyncEffectSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useAsyncEffect - cancels the previous effect when deps change"):
    val log       = cats.effect.Ref.unsafe[IO, List[Int]](Nil)
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        dep <- useState(0)
        // dep 0's effect is slow; dep 1's is fast, so dep 0 would record *after* dep 1 if not
        // cancelled on the deps change.
        _   <- useAsyncEffectWithDeps(dep.value): d =>
                 IO.sleep(if d == 0 then 100.millis else 10.millis) >> log.update(d :: _)
      yield <.button("x", ^.onClick --> dep.modState(_ + 1)).withRef(buttonRef)

    withRendered(comp()): _ =>
      for
        // No pre-click wait needed: on the deps change, `useSingleEffect` waits for dep 0's effect
        // to have started, then cancels it mid-sleep.
        _ <- act_(Simulate.click(buttonRef.unsafeGet())) // dep -> 1, cancels dep 0's effect
        // Wait past dep 0's would-be completion (100ms) to confirm it never recorded.
        _ <- act(IO.sleep(120.millis))
        r <- log.get
        _  = assertEquals(r, List(1))                    // only dep 1 ran; dep 0 was cancelled
      yield ()

  test("useAsyncEffect - runs the cleanup effect on deps change"):
    val log       = cats.effect.Ref.unsafe[IO, List[String]](Nil)
    val started1  = Deferred.unsafe[IO, Unit] // signalled when the dep-1 effect has run
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        dep <- useState(0)
        _   <- useAsyncEffectWithDeps(dep.value): d =>
                 (log.update(s"start$d" :: _) >>
                   (if d == 1 then started1.complete(()).void else IO.unit))
                   .as(log.update(s"cleanup$d" :: _))
      yield <.button("x", ^.onClick --> dep.modState(_ + 1)).withRef(buttonRef)

    withRendered(comp()): _ =>
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        // Wait deterministically until the dep-1 effect has run. `useSingleEffect`'s latch keeps the
        // order, so by then `start0 -> cleanup0 -> start1` have all happened.
        _ <- act(started1.get)
        r <- log.get
        _  = assertEquals(r.reverse, List("start0", "cleanup0", "start1"))
      yield ()

  test("useAsyncEffectOnMount - runs the effect on mount"):
    val ran = Deferred.unsafe[IO, Unit]

    val comp = ScalaFnComponent[Unit]: _ =>
      for _ <- useAsyncEffectOnMount(ran.complete(()).void)
      yield EmptyVdom

    // `act(ran.get)` only completes if the on-mount effect ran.
    withRendered(comp())(_ => act(ran.get))
