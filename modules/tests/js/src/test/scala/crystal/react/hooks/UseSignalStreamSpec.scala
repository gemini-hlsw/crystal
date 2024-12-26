// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import munit.FunSuite
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.test.ReactTestUtils2

class UseSignalStreamSuite extends FunSuite:

  test("useSignalStreamSuite"):
    println("HELLO")
    println(org.scalajs.dom.window.location)
    val comp = ScalaFnComponent[Unit]: _ =>
      <.div

    ReactTestUtils2.withRendered(comp()) { d =>
      assertEquals(d.toString, "<div></div>")
    }
