// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
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

class UseSignalStreamSuite extends CatsEffectSuite:

  test("useSignalStreamSuite"):
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

    import japgolly.scalajs.react.test.internal.WithDsl
    import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA
    import japgolly.scalajs.react.util.Effect.Async
    import japgolly.scalajs.react.test.TestDomWithRoot

    @inline def actAsync[F[_], A](body: => A)(implicit F: Async[F]): F[A] =
      ReactTestUtils2.actAsync(F.delay(body))

    def renderAsync[F[_], A](
      unmounted: A
    )(implicit F: Async[F], renderable: Renderable[A]): F[TestDomWithRoot] =
      F.flatMap(F.pure(ReactTestUtils2.withReactRoot.setup(implicitly, new WithDsl.Cleanup)))(
        root => F.map(actAsync(root.render(unmounted)))(_ => root.selectFirstChild())
      )

    for
      d <- renderAsync(outer())
      _ <- actAsync(Simulate.click(buttonRef.unsafeGet()))
      _ <- actAsync(d.unmount())
      r <- result.get
    yield assertEquals(r, List(0, 1))
