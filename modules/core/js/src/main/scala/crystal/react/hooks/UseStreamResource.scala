// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Resource
import crystal.*
import crystal.react.*
import crystal.react.reuse.Reuse
import crystal.react.syntax.pot.given
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA
import japgolly.scalajs.react.util.DefaultEffects.Sync as DefaultS

import scala.reflect.ClassTag

object UseStreamResource:

  private def buildStreamResource[D, A](
    streamResource: D => StreamResource[A],
    setState:       PotOption[A] => DefaultA[Unit]
  ): D => Resource[DefaultA, fs2.Stream[DefaultA, Unit]] =
    (deps: D) =>
      Resource
        .eval(setState(PotOption.pending))
        .flatMap: _ =>
          streamResource(deps)
            .map: stream =>
              fs2.Stream.eval(setState(PotOption.ReadyNone)) ++
                stream
                  .evalMap(v => setState(v.readySome))
                  .handleErrorWith: t =>
                    fs2.Stream.eval(setState(PotOption.error(t)))

  // START useStreamResource

  /**
   * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and drain
   * the stream by creating a fiber. Provides pulled values as a `PotOption[A]`. Will rerender when
   * the `PotOption` state changes. The fiber will be cancelled on unmount or deps change.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final def useStreamResource[D: Reusability, A](deps: => D)(
    streamResource: D => StreamResource[A]
  ): HookResult[PotOption[A]] =
    for
      state <- useState(PotOption.pending[A])
      _     <- useEffectStreamResourceWithDeps(deps):
                 buildStreamResource(streamResource, state.setStateAsync)
    yield state.value

  /**
   * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
   * fiber. Provides pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state
   * changes. The fiber will be cancelled on unmount.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamResourceOnMount[A](
    streamResource: StreamResource[A]
  ): HookResult[PotOption[A]] =
    useStreamResource(())(_ => streamResource)

  /**
   * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
   * pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state changes. The fiber
   * will be cancelled on unmount or deps change.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStream[D: Reusability, A](deps: => D)(
    stream: D => fs2.Stream[DefaultA, A]
  ): HookResult[PotOption[A]] =
    useStreamResource(deps)(deps => Resource.pure(stream(deps)))

  /**
   * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
   * `PotOption[A]`. Will rerender when the `PotOption` state changes. The fiber will be cancelled
   * on unmount.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamOnMount[A](stream: fs2.Stream[DefaultA, A]): HookResult[PotOption[A]] =
    useStreamResourceOnMount(Resource.pure(stream))

  // END useStreamResource

  // START useStreamResourceView

  /**
   * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and drain
   * the stream by creating a fiber. Provides pulled values as a `PotOption[View[A]]` so that the
   * value can also be changed locally. Will rerender when the `PotOption` state changes. The fiber
   * will be cancelled on unmount or deps change.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final def useStreamResourceView[D: Reusability, A](deps: => D)(
    streamResource: D => StreamResource[A]
  ): HookResult[PotOption[View[A]]] =
    for
      state <- useStateView(PotOption.pending[A])
      _     <- useEffectStreamResourceWithDeps(deps):
                 buildStreamResource(streamResource, state.set(_).to[DefaultA])
    yield state.toPotOptionView

  /**
   * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
   * fiber. Provides pulled values as a `PotOption[View[A]]` so that the value can also be changed
   * locally. Will rerender when the `PotOption` state changes. The fiber will be cancelled on
   * unmount.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamResourceViewOnMount[A](
    streamResource: StreamResource[A]
  ): HookResult[PotOption[View[A]]] =
    useStreamResourceView(())(_ => streamResource)

  /**
   * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
   * pulled values as a `PotOption[View[A]]` so that the value can also be changed locally. Will
   * rerender when the `PotOption` state changes. The fiber will be cancelled on unmount or deps
   * change.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamView[D: Reusability, A](deps: => D)(
    stream: D => fs2.Stream[DefaultA, A]
  ): HookResult[PotOption[View[A]]] =
    useStreamResourceView(deps)(deps => Resource.pure(stream(deps)))

  /**
   * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
   * `PotOption[View[A]]` so that the value can also be changed locally. Will rerender when the
   * `PotOption` state changes. The fiber will be cancelled on unmount.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamViewOnMount[A](
    stream: fs2.Stream[DefaultA, A]
  ): HookResult[PotOption[View[A]]] =
    useStreamResourceViewOnMount(Resource.pure(stream))

  // END useStreamResourceView

  // START useStreamResourceViewWithReuse

  /**
   * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and drain
   * the stream by creating a fiber. Provides pulled values as a `Reuse[PotOption[View[A]]` so that
   * the value can also be changed locally, reusable by value. Will rerender when the `PotOption`
   * state changes. The fiber will be cancelled on unmount or deps change.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final def useStreamResourceViewWithReuse[D: Reusability, A: ClassTag: Reusability](
    deps:           => D
  )(
    streamResource: D => StreamResource[A]
  ): HookResult[Reuse[PotOption[View[A]]]] =
    for
      state <- useStateViewWithReuse(PotOption.pending[A])
      _     <- useEffectStreamResourceWithDeps(deps):
                 buildStreamResource(streamResource, state.set(_).to[DefaultA])
    yield state.map(_.toPotOptionView)

  /**
   * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
   * fiber. Provides pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be
   * changed locally, reusable by value. Will rerender when the `PotOption` state changes. The fiber
   * will be cancelled on unmount.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamResourceViewWithReuseOnMount[A: ClassTag: Reusability](
    streamResource: StreamResource[A]
  ): HookResult[Reuse[PotOption[View[A]]]] =
    useStreamResourceViewWithReuse(())(_ => streamResource)

  /**
   * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
   * pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be changed locally,
   * reusable by value. Will rerender when the `PotOption` state changes. The fiber will be
   * cancelled on unmount or deps change.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamViewWithReuse[D: Reusability, A: ClassTag: Reusability](
    deps:   => D
  )(
    stream: D => fs2.Stream[DefaultA, A]
  ): HookResult[Reuse[PotOption[View[A]]]] =
    useStreamResourceViewWithReuse(deps)(deps => Resource.pure(stream(deps)))

  /**
   * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
   * `Reuse[PotOption[View[A]]` so that the value can also be changed locally, reusable by value.
   * Will rerender when the `PotOption` state changes. The fiber will be cancelled on unmount.
   *
   * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
   * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
   * received.
   */
  final inline def useStreamViewWithReuseOnMount[A: ClassTag: Reusability](
    stream: fs2.Stream[DefaultA, A]
  ): HookResult[Reuse[PotOption[View[A]]]] =
    useStreamResourceViewWithReuseOnMount(Resource.pure(stream))

  // END useStreamResourceViewWithReuse

  // *** The rest is to support builder-style hooks *** //

  private def hook[D: Reusability, A]: CustomHook[WithDeps[D, StreamResource[A]], PotOption[A]] =
    CustomHook.fromHookResult(input => useStreamResource(input.deps)(input.fromDeps))

  private def hookView[D: Reusability, A]
    : CustomHook[WithDeps[D, StreamResource[A]], PotOption[View[A]]] =
    CustomHook.fromHookResult(input => useStreamResourceView(input.deps)(input.fromDeps))

  private def hookReuseView[D: Reusability, A: ClassTag: Reusability]
    : CustomHook[WithDeps[D, StreamResource[A]], Reuse[PotOption[View[A]]]] =
    CustomHook.fromHookResult(input => useStreamResourceViewWithReuse(input.deps)(input.fromDeps))

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      //// BEGIN PLAIN METHODS

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state changes. The
       * fiber will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStream[D: Reusability, A](deps: => D)(
        stream: D => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResource(deps)(deps => Resource.pure(stream(deps)))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `PotOption[View[A]]` so that the value can also be changed locally. Will
       * rerender when the `PotOption` state changes. The fiber will be cancelled on unmount or deps
       * change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamView[D: Reusability, A](deps: => D)(
        stream: D => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceView(deps)(deps => Resource.pure(stream(deps)))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be changed
       * locally, reusable by value. Will rerender when the `PotOption` state changes. The fiber
       * will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewWithReuse[D: Reusability, A: ClassTag: Reusability](
        deps:   => D
      )(
        stream: D => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuse(deps)(deps => Resource.pure(stream(deps)))

      // END PLAIN METHODS

      // BEGIN PLAIN "ON MOUNT" METHODS

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `PotOption[A]`. Will rerender when the `PotOption` state changes. The fiber will be
       * cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamOnMount[A](
        stream: fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceOnMount(Resource.pure(stream))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `PotOption[View[A]]` so that the value can also be changed locally. Will rerender when the
       * `PotOption` state changes. The fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewOnMount[A](
        stream: fs2.Stream[DefaultA, A]
      )(using
        step:   Step
      ): step.Next[PotOption[View[A]]] =
        useStreamResourceViewOnMount(Resource.pure(stream))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `Reuse[PotOption[View[A]]` so that the value can also be changed locally, reusable by
       * value. Will rerender when the `PotOption` state changes. The fiber will be cancelled on
       * unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewWithReuseOnMount[A: ClassTag: Reusability](
        stream: fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseOnMount(Resource.pure(stream))

      // END PLAIN "ON MOUNT" METHODS

      // BEGIN "BY" METHODS

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state changes. The
       * fiber will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamBy[D: Reusability, A](deps: Ctx => D)(
        stream: Ctx => D => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `PotOption[View[A]]` so that the value can also be changed locally. Will
       * rerender when the `PotOption` state changes. The fiber will be cancelled on unmount or deps
       * change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewBy[D: Reusability, A](deps: Ctx => D)(
        stream: Ctx => D => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceViewBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be changed
       * locally, reusable by value. Will rerender when the `PotOption` state changes. The fiber
       * will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:   Ctx => D
      )(
        stream: Ctx => D => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `PotOption[A]`. Will rerender when the `PotOption` state changes. The fiber will be
       * cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamOnMountBy[A](
        stream: Ctx => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceOnMountBy(ctx => Resource.pure(stream(ctx)))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `PotOption[View[A]]` so that the value can also be changed locally. Will rerender when the
       * `PotOption` state changes. The fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewOnMountBy[A](
        stream: Ctx => fs2.Stream[DefaultA, A]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceViewOnMountBy(ctx => Resource.pure(stream(ctx)))

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `Reuse[PotOption[View[A]]` so that the value can also be changed locally, reusable by
       * value. Will rerender when the `PotOption` state changes. The fiber will be cancelled on
       * unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewWithReuseOnMountBy[A: ClassTag: Reusability](
        stream: Ctx => fs2.Stream[DefaultA, A]
      )(using
        step:   Step
      ): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseOnMountBy(ctx => Resource.pure(stream(ctx)))

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM METHODS

      //// BEGIN STREAM RESOURCE METHODS

      // BEGIN PLAIN METHODS

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a `PotOption[A]`. Will
       * rerender when the `PotOption` state changes. The fiber will be cancelled on unmount or deps
       * change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResource[D: Reusability, A](deps: => D)(
        streamResource: D => StreamResource[A]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceBy(_ => deps)(_ => streamResource)

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a `PotOption[View[A]]` so
       * that the value can also be changed locally. Will rerender when the `PotOption` state
       * changes. The fiber will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceView[D: Reusability, A](deps: => D)(
        streamResource: D => StreamResource[A]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceViewBy(_ => deps)(_ => streamResource)

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a
       * `Reuse[PotOption[View[A]]` so that the value can also be changed locally, reusable by
       * value. Will rerender when the `PotOption` state changes. The fiber will be cancelled on
       * unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewWithReuse[D: Reusability, A: ClassTag: Reusability](
        deps:           => D
      )(
        streamResource: D => StreamResource[A]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseBy(_ => deps)(_ => streamResource)

      // END PLAIN METHODS

      // BEGIN PLAIN "ON MOUNT" METHODS

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state
       * changes. The fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceOnMount[A](
        streamResource: StreamResource[A]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceOnMountBy(_ => streamResource)

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `PotOption[View[A]]` so that the value can also be
       * changed locally. Will rerender when the `PotOption` state changes. The fiber will be
       * cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewOnMount[A](
        streamResource: StreamResource[A]
      )(using
        step:           Step
      ): step.Next[PotOption[View[A]]] =
        useStreamResourceViewOnMountBy(_ => streamResource)

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be
       * changed locally, reusable by value. Will rerender when the `PotOption` state changes. The
       * fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewWithReuseOnMount[A: ClassTag: Reusability](
        streamResource: StreamResource[A]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseOnMountBy(_ => streamResource)

      // END PLAIN "ON MOUNT" METHODS

      // BEGIN "BY" METHODS

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a `PotOption[A]`. Will
       * rerender when the `PotOption` state changes. The fiber will be cancelled on unmount or deps
       * change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceBy[D: Reusability, A](deps: Ctx => D)(
        streamResource: Ctx => D => StreamResource[A]
      )(using step: Step): step.Next[PotOption[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a `PotOption[View[A]]` so
       * that the value can also be changed locally. Will rerender when the `PotOption` state
       * changes. The fiber will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewBy[D: Reusability, A](deps: Ctx => D)(
        streamResource: Ctx => D => StreamResource[A]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hookView[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a
       * `Reuse[PotOption[View[A]]` so that the value can also be changed locally, reusable by
       * value. Will rerender when the `PotOption` state changes. The fiber will be cancelled on
       * unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:           Ctx => D
      )(
        streamResource: Ctx => D => StreamResource[A]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        api.customBy { ctx =>
          val hookInstance = hookReuseView[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state
       * changes. The fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceOnMountBy[A](
        streamResource: Ctx => StreamResource[A]
      )(using step: Step): step.Next[PotOption[A]] = // () has Reusability = always.
        useStreamResourceBy(_ => ())(ctx => _ => streamResource(ctx))

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `PotOption[View[A]]` so that the value can also be
       * changed locally. Will rerender when the `PotOption` state changes. The fiber will be
       * cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewOnMountBy[A](
        streamResource: Ctx => StreamResource[A]
      )(using step: Step): step.Next[PotOption[View[A]]] = // () has Reusability = always.
        useStreamResourceViewBy(_ => ())(ctx => _ => streamResource(ctx))

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be
       * changed locally, reusable by value. Will rerender when the `PotOption` state changes. The
       * fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewWithReuseOnMountBy[A: ClassTag: Reusability](
        streamResource: Ctx => StreamResource[A]
      )(using
        step:           Step
      ): step.Next[Reuse[PotOption[View[A]]]] = // () has Reusability = always.
        useStreamResourceViewWithReuseBy(_ => ())(ctx => _ => streamResource(ctx))

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM RESOURCE METHODS
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {
      //// BEGIN STREAM METHODS

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state changes. The
       * fiber will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamBy[D: Reusability, A](deps: CtxFn[D])(
        stream: CtxFn[D => fs2.Stream[DefaultA, A]]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `PotOption[View[A]]` so that the value can also be changed locally. Will
       * rerender when the `PotOption` state changes. The fiber will be cancelled on unmount or deps
       * change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewBy[D: Reusability, A](deps: CtxFn[D])(
        stream: CtxFn[D => fs2.Stream[DefaultA, A]]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceViewBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
       * pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be changed
       * locally, reusable by value. Will rerender when the `PotOption` state changes. The fiber
       * will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:   CtxFn[D]
      )(
        stream: CtxFn[D => fs2.Stream[DefaultA, A]]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `PotOption[A]`. Will rerender when the `PotOption` state changes. The fiber will be
       * cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamOnMountBy[A](
        stream: CtxFn[fs2.Stream[DefaultA, A]]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `PotOption[View[A]]` so that the value can also be changed locally. Will rerender when the
       * `PotOption` state changes. The fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewOnMountBy[A](
        stream: CtxFn[fs2.Stream[DefaultA, A]]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceViewOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      /**
       * Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
       * `Reuse[PotOption[View[A]]` so that the value can also be changed locally, reusable by
       * value. Will rerender when the `PotOption` state changes. The fiber will be cancelled on
       * unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamViewWithReuseOnMountBy[A: ClassTag: Reusability](
        stream: CtxFn[fs2.Stream[DefaultA, A]]
      )(using
        step:   Step
      ): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM METHODS

      //// BEGIN STREAM RESOURCE METHODS

      // BEGIN "BY" METHODS

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a `PotOption[A]`. Will
       * rerender when the `PotOption` state changes. The fiber will be cancelled on unmount or deps
       * change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource: CtxFn[D => StreamResource[A]]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a `PotOption[View[A]]` so
       * that the value can also be changed locally. Will rerender when the `PotOption` state
       * changes. The fiber will be cancelled on unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource: CtxFn[D => StreamResource[A]]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceViewBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. Provides pulled values as a
       * `Reuse[PotOption[View[A]]` so that the value can also be changed locally, reusable by
       * value. Will rerender when the `PotOption` state changes. The fiber will be cancelled on
       * unmount or deps change.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:           CtxFn[D]
      )(
        streamResource: CtxFn[D => StreamResource[A]]
      )(using step: Step): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `PotOption[A]`. Will rerender when the `PotOption` state
       * changes. The fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceOnMountBy[A](
        streamResource: CtxFn[StreamResource[A]]
      )(using step: Step): step.Next[PotOption[A]] =
        useStreamResourceOnMountBy(step.squash(streamResource)(_))

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `PotOption[View[A]]` so that the value can also be
       * changed locally. Will rerender when the `PotOption` state changes. The fiber will be
       * cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewOnMountBy[A](
        streamResource: CtxFn[StreamResource[A]]
      )(using step: Step): step.Next[PotOption[View[A]]] =
        useStreamResourceViewOnMountBy(step.squash(streamResource)(_))

      /**
       * Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
       * fiber. Provides pulled values as a `Reuse[PotOption[View[A]]` so that the value can also be
       * changed locally, reusable by value. Will rerender when the `PotOption` state changes. The
       * fiber will be cancelled on unmount.
       *
       * The value will be `Pending` when the stream hasn't been mounted yet, `ReadyNone` when the
       * stream is mounted but no value received yet, and `ReadySome(a)` when `a` is the last value
       * received.
       */
      final def useStreamResourceViewWithReuseOnMountBy[A: ClassTag: Reusability](
        streamResource: CtxFn[StreamResource[A]]
      )(using
        step:           Step
      ): step.Next[Reuse[PotOption[View[A]]]] =
        useStreamResourceViewWithReuseOnMountBy(step.squash(streamResource)(_))

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM RESOURCE METHODS
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtStreamResourceView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtStreamResourceView2[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx,
                                                                                            CtxFn
    ]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
