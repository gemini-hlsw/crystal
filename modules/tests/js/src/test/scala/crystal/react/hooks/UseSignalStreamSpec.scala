// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

// import munit.FunSuite
import munit.CatsEffectSuite
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.test.ReactTestUtils2
import cats.syntax.all.*
import org.scalajs.dom
import japgolly.scalajs.react.test.Simulate
import cats.effect.IO
import scala.concurrent.duration.*

class UseSignalStreamSuite extends CatsEffectSuite:

  // test("SEE If this runs"):
  //   IO.println("*******RUNSSSSS*********") >>
  //     IO(assertEquals(10, 11)) >>
  //     assertIO(IO(42), 43)

  test("useSignalStreamSuite"):
    var result: List[Int] = List.empty

    def consume(stream: fs2.Stream[IO, Int]): IO[Unit] =
      stream.compile.toList.map:
        result = _

    val inner = ScalaFnComponent[Int]: props =>
      for
        stream <- useSignalStream(props)
        _      <- useEffectWhenDepsReady(stream)(s => consume(s.evalTap(IO.println(_))))
      yield
      // println(s"props: $props")
      EmptyVdom

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
        IO.sleep(100.millis) >> IO.println(" FDEFEWFWE") >> IO {
          Simulate.click(buttonRef.unsafeGet())
          d.unmount()
          assertEquals(result, List(0, 1))
        }
    // .onError(IO.println(_))
    // .unsafeRunAndForget()
