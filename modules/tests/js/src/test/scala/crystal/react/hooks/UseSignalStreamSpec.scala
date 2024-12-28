// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import munit.CatsEffectSuite
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.test.ReactTestUtils2
import cats.syntax.all.*
import org.scalajs.dom
import japgolly.scalajs.react.test.Simulate
import cats.effect.IO
import scala.concurrent.duration.*
import cats.effect.kernel.Deferred

class UseSignalStreamSuite extends CatsEffectSuite:

  test("useSignalStreamSuite"):
    // Completes when the stream ready and we then start the test.
    val readyLatch: Deferred[IO, Unit]  = Deferred.unsafe[IO, Unit]
    // Completes when the stream terminates
    val result: Deferred[IO, List[Int]] = Deferred.unsafe[IO, List[Int]]

    def consume(stream: fs2.Stream[IO, Int]): IO[Unit] =
      stream.compile.toList
        .flatMap(l => result.complete(l))
        .void

    val inner = ScalaFnComponent[Int]: props =>
      for
        stream <- useSignalStream(props)
        _      <- useEffectWhenDepsReady(stream): s =>
                    readyLatch.complete(()) >> consume(s)
      yield EmptyVdom

    val buttonRef = Ref[dom.HTMLButtonElement]

    val outer = ScalaFnComponent[Unit]: _ =>
      for state <- useState(0)
      yield React.Fragment(
        inner(state.value),
        <.button(^.onClick --> state.modState(_ + 1)).withRef(buttonRef)
      )

    ReactTestUtils2
      .withRendered(outer())
      .async: d =>
        readyLatch.get >>
          IO {
            Simulate.click(buttonRef.unsafeGet())
            d.unmount()
          } >>
          result.get.map(r => assertEquals(_, List(0, 1)))
