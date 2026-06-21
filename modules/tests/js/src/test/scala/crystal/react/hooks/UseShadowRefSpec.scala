// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useShadowRef` against its README description: "keeps the value in a ref with the
// latest value [...] useful when a stable callback wants to access a value that can change, but we
// don't want to redefine the callback in each render."
class UseShadowRefSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useShadowRef - a stable callback reads the latest value through the ref"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        s        <- useState(0)
        shadow   <- useShadowRef(s.value)
        readBack <- useState(-1)
        // A stable callback (created once, never redefined across renders). It reads `shadow` at
        // call time, so it always sees the latest value — exactly the README use case.
        reader   <- useCallback(shadow.get.flatMap(readBack.setState))
      yield <.button(
        (s.value, readBack.value).toString,
        ^.onClick --> s.modState(_ + 1),
        ^.onDoubleClick --> reader
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("(0,-1)")
      for
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // stable reader sees current 0
        _  = d.innerHTML.assert("(0,0)")
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // s -> 1
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // s -> 2
        _  = d.innerHTML.assert("(2,0)")
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // same stable reader now sees 2
        _  = d.innerHTML.assert("(2,2)")
      yield ()
