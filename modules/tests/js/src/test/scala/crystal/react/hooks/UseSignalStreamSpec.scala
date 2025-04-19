// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.IO
import cats.effect.kernel.Deferred
import cats.syntax.all.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils2
import japgolly.scalajs.react.test.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

class UseSignalStreamSpec extends CatsEffectSuite:
  import ReactTestUtils2.*

  test("useSignalStream - build signal stream"):
    // Completes when the stream terminates
    val result: Deferred[IO, List[Int]] = Deferred.unsafe[IO, List[Int]]

    def consume(stream: fs2.Stream[IO, Int]): IO[Unit] =
      stream.compile.toList
        .flatMap(l => result.complete(l))
        .void

    val inner = ScalaFnComponent[Int]: props =>
      for
        stream <- useSignalStream(props)
        _      <- useEffectWhenDepsReady(stream)(consume(_))
      yield EmptyVdom

    val buttonRef = Ref[dom.HTMLButtonElement]

    val outer = ScalaFnComponent[Unit]: _ =>
      for state <- useState(0)
      yield React.Fragment(
        inner(state.value),
        <.button(^.onClick --> state.modState(_ + 1)).withRef(buttonRef)
      )

    for
      _ <- withRendered(outer()): _ =>
             act_(Simulate.click(buttonRef.unsafeGet()))
      r <- result.get
    yield assertEquals(r, List(0, 1))
