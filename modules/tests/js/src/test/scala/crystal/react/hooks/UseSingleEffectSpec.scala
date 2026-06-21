// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

import scala.concurrent.duration.*

// Exercises `useSingleEffect` against its README description: "Provides a context in which to run a
// single effect at a time. When a new effect is submitted, the previous one is canceled."
class UseSingleEffectSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useSingleEffect - submitting a new effect cancels the previously running one"):
    val log       = cats.effect.Ref.unsafe[IO, List[Int]](Nil)
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        trigger <- useState(0)
        se      <- useSingleEffect
        // Each `trigger` change submits a new effect; submission 0 is slow, submission 1 is fast.
        _       <- useEffectWithDeps(trigger.value): n =>
                     se.value.submit(IO.sleep(if n == 0 then 100.millis
                     else 10.millis) >> log.update(n :: _))
      yield <.button("x", ^.onClick --> trigger.modState(_ + 1)).withRef(buttonRef)

    withRendered(comp()): _ =>
      for
        // No pre-click wait needed: the new submission waits for submission 0 to have started, then
        // cancels it mid-sleep.
        _ <- act_(Simulate.click(buttonRef.unsafeGet())) // submission 1 -> cancels submission 0
        // Wait past submission 0's would-be completion (100ms) to confirm it never recorded.
        _ <- act(IO.sleep(120.millis))
        r <- log.get
        _  = assertEquals(r, List(1))                    // only the latest submission ran
      yield ()
