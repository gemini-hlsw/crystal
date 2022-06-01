package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.Resource
import cats.syntax.all._
import crystal.Pot
import crystal.implicits._
import crystal.react.ReuseView
import crystal.react.View
import crystal.react.implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

import scala.reflect.ClassTag

object UseStreamResource {

  protected def hookBase[D: Reusability, A] =
    CustomHook[WithDeps[D, StreamResource[A]]]
      .useState(Pot.pending[A])
      .useSerialState(none[Deferred[DefaultA, Unit]])
      .useResourceBy((props, _, _) => props.deps) { (props, state, latchRef) => deps =>
        for {
          latch  <- Resource.eval(Deferred[DefaultA, Unit])
          _      <- Resource.eval(latchRef.setStateAsync(latch.some))
          _      <- Resource.eval(state.setStateAsync(Pot.pending))
          stream <- props.fromDeps(deps)
          fiber  <- Resource
                      .make(
                        stream
                          .evalMap(v => state.setStateAsync(v.ready))
                          .compile
                          .drain
                          .handleErrorWith(state.setStateAsync.compose(Pot.error))
                          .start
                      )(_.cancel)
                      .evalTap(_ => latch.complete(()))
        } yield fiber
      }

  protected def buildResult[A](
    resource: Pot[AsyncUnitFiber],
    state:    Hooks.UseState[Pot[A]]
  ): Pot[A] =
    resource.flatMap(_ => state.value)

  protected def buildView[A](
    resource:        Pot[AsyncUnitFiber],
    state:           Hooks.UseState[Pot[A]],
    delayedCallback: (Pot[A] => DefaultS[Unit]) => DefaultS[Unit]
  ): Pot[View[A]] =
    resource.flatMap(_ =>
      state.value.map { a =>
        View[A](
          a,
          (f: A => A, cb: A => DefaultS[Unit]) =>
            state.modState(_.map(f)) >> delayedCallback(_.toOption.foldMap(cb))
        )
      }
    )

  protected def buildReuseView[A: ClassTag: Reusability](
    resource:        Pot[AsyncUnitFiber],
    state:           Hooks.UseState[Pot[A]],
    delayedCallback: (Pot[A] => DefaultS[Unit]) => DefaultS[Unit]
  ): Pot[ReuseView[A]] =
    buildView(resource, state, delayedCallback).map(_.reuseByValue)

  protected def toSync[A](
    value: A,
    latch: UseSerialState[Option[Deferred[DefaultA, Unit]]]
  ): SyncValue[A] =
    SyncValue(value, latch.value.map(_.map(_.get)))

  def hook[D: Reusability, A] =
    hookBase[D, A]
      .buildReturning((_, state, _, resource) => buildResult(resource, state))

  def hookWithSync[D: Reusability, A] =
    hookBase[D, A]
      .buildReturning((_, state, latch, resource) => toSync(buildResult(resource, state), latch))

  def hookView[D: Reusability, A] =
    hookBase[D, A]
      .useStateCallbackBy((_, state, _, _) => state)
      .buildReturning { (_, state, _, resource, delayedCallback) =>
        buildView(resource, state, delayedCallback)
      }

  def hookViewWithSync[D: Reusability, A] =
    hookBase[D, A]
      .useStateCallbackBy((_, state, _, _) => state)
      .buildReturning { (_, state, latch, resource, delayedCallback) =>
        toSync(buildView(resource, state, delayedCallback), latch)
      }

  def hookReuseView[D: Reusability, A: ClassTag: Reusability] =
    hookBase[D, A]
      .useStateCallbackBy((_, state, _, _) => state)
      .buildReturning { (_, state, _, resource, delayedCallback) =>
        buildReuseView(resource, state, delayedCallback)
      }

  def hookReuseViewWithSync[D: Reusability, A: ClassTag: Reusability] =
    hookBase[D, A]
      .useStateCallbackBy((_, state, _, _) => state)
      .buildReturning { (_, state, latch, resource, delayedCallback) =>
        toSync(buildReuseView(resource, state, delayedCallback), latch)
      }

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      //// BEGIN STREAM METHODS

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStream[D: Reusability, A](deps: => D)(
        stream:                                    D => fs2.Stream[DefaultA, A]
      )(implicit step:                             Step): step.Next[Pot[A]] =
        useStreamResource(deps)(deps => Resource.pure(stream(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamWithSync[D: Reusability, A](deps: => D)(
        stream:                                            D => fs2.Stream[DefaultA, A]
      )(implicit step:                                     Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSync(deps)(deps => Resource.pure(stream(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamView[D: Reusability, A](deps: => D)(
        stream:                                        D => fs2.Stream[DefaultA, A]
      )(implicit step:                                 Step): step.Next[Pot[View[A]]] =
        useStreamResourceView(deps)(deps => Resource.pure(stream(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithSync[D: Reusability, A](deps: => D)(
        stream:                                                D => fs2.Stream[DefaultA, A]
      )(implicit step:                                         Step): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSync(deps)(deps => Resource.pure(stream(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithReuse[D: Reusability, A: ClassTag: Reusability](
        deps:          => D
      )(
        stream:        D => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuse(deps)(deps => Resource.pure(stream(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithReuseAndSync[D: Reusability, A: ClassTag: Reusability](
        deps:          => D
      )(
        stream:        D => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSync(deps)(deps => Resource.pure(stream(deps)))

      // END PLAIN METHODS

      // BEGIN PLAIN "ON MOUNT" METHODS

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamOnMount[A](
        stream:        fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[Pot[A]] =
        useStreamResourceOnMount(Resource.pure(stream))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamWithSyncOnMount[A](
        stream:        fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncOnMount(Resource.pure(stream))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewOnMount[A](
        stream: fs2.Stream[DefaultA, A]
      )(implicit
        step:   Step
      ): step.Next[Pot[View[A]]] =
        useStreamResourceViewOnMount(Resource.pure(stream))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithSyncOnMount[A](
        stream: fs2.Stream[DefaultA, A]
      )(implicit
        step:   Step
      ): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncOnMount(Resource.pure(stream))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithReuseOnMount[A: ClassTag: Reusability](
        stream:        fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseOnMount(Resource.pure(stream))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithReuseAndSyncOnMount[A: ClassTag: Reusability](
        stream:        fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncOnMount(Resource.pure(stream))

      // END PLAIN "ON MOUNT" METHODS

      // BEGIN "BY" METHODS

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamBy[D: Reusability, A](deps: Ctx => D)(
        stream:                                      Ctx => D => fs2.Stream[DefaultA, A]
      )(implicit step:                               Step): step.Next[Pot[A]] =
        useStreamResourceBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamWithSyncBy[D: Reusability, A](deps: Ctx => D)(
        stream:                                              Ctx => D => fs2.Stream[DefaultA, A]
      )(implicit step:                                       Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewBy[D: Reusability, A](deps: Ctx => D)(
        stream:                                          Ctx => D => fs2.Stream[DefaultA, A]
      )(implicit step:                                   Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithSyncBy[D: Reusability, A](deps: Ctx => D)(
        stream:                                                  Ctx => D => fs2.Stream[DefaultA, A]
      )(implicit step:                                           Step): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:          Ctx => D
      )(
        stream:        Ctx => D => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithReuseAndSyncBy[D: Reusability, A: ClassTag: Reusability](
        deps:          Ctx => D
      )(
        stream:        Ctx => D => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncBy(deps)(ctx =>
          deps => Resource.pure(stream(ctx)(deps))
        )

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamOnMountBy[A](
        stream:        Ctx => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[Pot[A]] =
        useStreamResourceOnMountBy(ctx => Resource.pure(stream(ctx)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamWithSyncOnMountBy[A](
        stream:        Ctx => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncOnMountBy(ctx => Resource.pure(stream(ctx)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewOnMountBy[A](
        stream:        Ctx => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewOnMountBy(ctx => Resource.pure(stream(ctx)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithSyncOnMountBy[A](
        stream:        Ctx => fs2.Stream[DefaultA, A]
      )(implicit step: Step): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncOnMountBy(ctx => Resource.pure(stream(ctx)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithReuseOnMountBy[A: ClassTag: Reusability](
        stream: Ctx => fs2.Stream[DefaultA, A]
      )(implicit
        step:   Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseOnMountBy(ctx => Resource.pure(stream(ctx)))

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithReuseAndSyncOnMountBy[A: ClassTag: Reusability](
        stream: Ctx => fs2.Stream[DefaultA, A]
      )(implicit
        step:   Step
      ): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncOnMountBy(ctx => Resource.pure(stream(ctx)))

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM METHODS

      //// BEGIN STREAM RESOURCE METHODS

      // BEGIN PLAIN METHODS

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResource[D: Reusability, A](deps: => D)(
        streamResource:                                    D => StreamResource[A]
      )(implicit step:                                     Step): step.Next[Pot[A]] =
        useStreamResourceBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceWithSync[D: Reusability, A](deps: => D)(
        streamResource:                                            D => StreamResource[A]
      )(implicit step:                                             Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceView[D: Reusability, A](deps: => D)(
        streamResource:                                        D => StreamResource[A]
      )(implicit step:                                         Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithSync[D: Reusability, A](deps: => D)(
        streamResource:                                                D => StreamResource[A]
      )(implicit step:                                                 Step): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuse[D: Reusability, A: ClassTag: Reusability](
        deps:           => D
      )(
        streamResource: D => StreamResource[A]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(_ => deps)(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuseAndSync[D: Reusability, A: ClassTag: Reusability](
        deps:           => D
      )(
        streamResource: D => StreamResource[A]
      )(implicit step:  Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncBy(_ => deps)(_ => streamResource)

      // END PLAIN METHODS

      // BEGIN PLAIN "ON MOUNT" METHODS

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceOnMount[A](
        streamResource: StreamResource[A]
      )(implicit step:  Step): step.Next[Pot[A]] =
        useStreamResourceOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceWithSyncOnMount[A](
        streamResource: StreamResource[A]
      )(implicit step:  Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewOnMount[A](
        streamResource: StreamResource[A]
      )(implicit
        step:           Step
      ): step.Next[Pot[View[A]]] =
        useStreamResourceViewOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithSyncOnMount[A](
        streamResource: StreamResource[A]
      )(implicit
        step:           Step
      ): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseOnMount[A: ClassTag: Reusability](
        streamResource: StreamResource[A]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseOnMountBy(_ => streamResource)

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseAndSyncOnMount[A: ClassTag: Reusability](
        streamResource: StreamResource[A]
      )(implicit step:  Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncOnMountBy(_ => streamResource)

      // END PLAIN "ON MOUNT" METHODS

      // BEGIN "BY" METHODS

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceBy[D: Reusability, A](deps: Ctx => D)(
        streamResource:                                      Ctx => D => StreamResource[A]
      )(implicit step:                                       Step): step.Next[Pot[A]] =
        api.customBy { ctx =>
          val hookInstance = hook[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceWithSyncBy[D: Reusability, A](deps: Ctx => D)(
        streamResource:                                              Ctx => D => StreamResource[A]
      )(implicit step:                                               Step): step.Next[SyncValue[Pot[A]]] =
        api.customBy { ctx =>
          val hookInstance = hookWithSync[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewBy[D: Reusability, A](deps: Ctx => D)(
        streamResource:                                          Ctx => D => StreamResource[A]
      )(implicit step:                                           Step): step.Next[Pot[View[A]]] =
        api.customBy { ctx =>
          val hookInstance = hookView[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithSyncBy[D: Reusability, A](deps: Ctx => D)(
        streamResource:                                                  Ctx => D => StreamResource[A]
      )(implicit step:                                                   Step): step.Next[SyncValue[Pot[View[A]]]] =
        api.customBy { ctx =>
          val hookInstance = hookViewWithSync[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:           Ctx => D
      )(
        streamResource: Ctx => D => StreamResource[A]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        api.customBy { ctx =>
          val hookInstance = hookReuseView[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuseAndSyncBy[D: Reusability, A: ClassTag: Reusability](
        deps:           Ctx => D
      )(
        streamResource: Ctx => D => StreamResource[A]
      )(implicit step:  Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        api.customBy { ctx =>
          val hookInstance = hookReuseViewWithSync[D, A]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceOnMountBy[A](
        streamResource: Ctx => StreamResource[A]
      )(implicit step:  Step): step.Next[Pot[A]] = // () has Reusability = always.
        useStreamResourceBy(_ => ())(ctx => _ => streamResource(ctx))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceWithSyncOnMountBy[A](
        streamResource: Ctx => StreamResource[A]
      )(implicit step:  Step): step.Next[SyncValue[Pot[A]]] = // () has Reusability = always.
        useStreamResourceWithSyncBy(_ => ())(ctx => _ => streamResource(ctx))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewOnMountBy[A](
        streamResource: Ctx => StreamResource[A]
      )(implicit step:  Step): step.Next[Pot[View[A]]] = // () has Reusability = always.
        useStreamResourceViewBy(_ => ())(ctx => _ => streamResource(ctx))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithSyncOnMountBy[A](
        streamResource: Ctx => StreamResource[A]
      )(implicit step:  Step): step.Next[SyncValue[Pot[View[A]]]] = // () has Reusability = always.
        useStreamResourceViewWithSyncBy(_ => ())(ctx => _ => streamResource(ctx))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseOnMountBy[A: ClassTag: Reusability](
        streamResource: Ctx => StreamResource[A]
      )(implicit
        step:           Step
      ): step.Next[Pot[ReuseView[A]]] = // () has Reusability = always.
        useStreamResourceViewWithReuseBy(_ => ())(ctx => _ => streamResource(ctx))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseAndSyncOnMountBy[A: ClassTag: Reusability](
        streamResource: Ctx => StreamResource[A]
      )(implicit
        step:           Step
      ): step.Next[SyncValue[Pot[ReuseView[A]]]] = // () has Reusability = always.
        useStreamResourceViewWithReuseAndSyncBy(_ => ())(ctx => _ => streamResource(ctx))

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM RESOURCE METHODS
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {
      //// BEGIN STREAM METHODS

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamBy[D: Reusability, A](deps: CtxFn[D])(
        stream:                                      CtxFn[D => fs2.Stream[DefaultA, A]]
      )(implicit step:                               Step): step.Next[Pot[A]] =
        useStreamResourceBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamWithSyncBy[D: Reusability, A](deps: CtxFn[D])(
        stream:                                              CtxFn[D => fs2.Stream[DefaultA, A]]
      )(implicit step:                                       Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewBy[D: Reusability, A](deps: CtxFn[D])(
        stream:                                          CtxFn[D => fs2.Stream[DefaultA, A]]
      )(implicit step:                                   Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      def useStreamViewWithSyncBy[D: Reusability, A](deps: CtxFn[D])(
        stream:                                            CtxFn[D => fs2.Stream[DefaultA, A]]
      )(implicit step:                                     Step): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:          CtxFn[D]
      )(
        stream:        CtxFn[D => fs2.Stream[DefaultA, A]]
      )(implicit step: Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount or when deps change. Provides
        * pulled values as a `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be
        * cancelled on unmount or deps change.
        */
      final def useStreamViewWithReuseAndSyncBy[D: Reusability, A: ClassTag: Reusability](
        deps:          CtxFn[D]
      )(
        stream:        CtxFn[D => fs2.Stream[DefaultA, A]]
      )(implicit step: Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamOnMountBy[A](
        stream:        CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit step: Step): step.Next[Pot[A]] =
        useStreamResourceOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamWithSyncOnMountBy[A](
        stream:        CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit step: Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewOnMountBy[A](
        stream:        CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit step: Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithSyncOnMountBy[A](
        stream: CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit
        step:   Step
      ): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithReuseOnMountBy[A: ClassTag: Reusability](
        stream: CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit
        step:   Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      /** Drain a `fs2.Stream[Async, A]` by creating a fiber on mount. Provides pulled values as a
        * `Pot[A]`. Will rerender when the `Pot` state changes. The fiber will be cancelled on
        * unmount.
        */
      final def useStreamViewWithReuseAndSyncOnMountBy[A: ClassTag: Reusability](
        stream: CtxFn[fs2.Stream[DefaultA, A]]
      )(implicit
        step:   Step
      ): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, A]](step.squash(stream)(ctx))
        )

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM METHODS

      //// BEGIN STREAM RESOURCE METHODS

      // BEGIN "BY" METHODS

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource:                                      CtxFn[D => StreamResource[A]]
      )(implicit step:                                       Step): step.Next[Pot[A]] =
        useStreamResourceBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceWithSyncBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource:                                              CtxFn[D => StreamResource[A]]
      )(implicit step:                                               Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource:                                          CtxFn[D => StreamResource[A]]
      )(implicit step:                                           Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      def useStreamResourceViewWithSyncBy[D: Reusability, A](deps: CtxFn[D])(
        streamResource:                                            CtxFn[D => StreamResource[A]]
      )(implicit step:                                             Step): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuseBy[D: Reusability, A: ClassTag: Reusability](
        deps:           CtxFn[D]
      )(
        streamResource: CtxFn[D => StreamResource[A]]
      )(implicit step:  Step): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount or when dependencies change, and
        * drain the stream by creating a fiber. Provides pulled values as a `Pot[View[A]]` so that
        * the value can also be changed locally. Will rerender when the `Pot` state changes. The
        * fiber will be cancelled and the resource closed on unmount or deps change.
        */
      final def useStreamResourceViewWithReuseAndSyncBy[D: Reusability, A: ClassTag: Reusability](
        deps:           CtxFn[D]
      )(
        streamResource: CtxFn[D => StreamResource[A]]
      )(implicit step:  Step): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncBy(step.squash(deps)(_))(
          step.squash(streamResource)(_)
        )

      // END "BY" METHODS

      // BEGIN "BY" "ON MOUNT" METHODS

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceOnMountBy[A](
        streamResource: CtxFn[StreamResource[A]]
      )(implicit step:  Step): step.Next[Pot[A]] =
        useStreamResourceOnMountBy(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceWithSyncOnMountBy[A](
        streamResource: CtxFn[StreamResource[A]]
      )(implicit step:  Step): step.Next[SyncValue[Pot[A]]] =
        useStreamResourceWithSyncOnMountBy(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewOnMountBy[A](
        streamResource: CtxFn[StreamResource[A]]
      )(implicit step:  Step): step.Next[Pot[View[A]]] =
        useStreamResourceViewOnMountBy(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithSyncOnMountBy[A](
        streamResource: CtxFn[StreamResource[A]]
      )(implicit
        step:           Step
      ): step.Next[SyncValue[Pot[View[A]]]] =
        useStreamResourceViewWithSyncOnMountBy(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseOnMountBy[A: ClassTag: Reusability](
        streamResource: CtxFn[StreamResource[A]]
      )(implicit
        step:           Step
      ): step.Next[Pot[ReuseView[A]]] =
        useStreamResourceViewWithReuseOnMountBy(step.squash(streamResource)(_))

      /** Open a `Resource[Async, fs.Stream[Async, A]]` on mount, and drain the stream by creating a
        * fiber. Provides pulled values as a `Pot[View[A]]` so that the value can also be changed
        * locally. Will rerender when the `Pot` state changes. The fiber will be cancelled and the
        * resource closed on unmount.
        */
      final def useStreamResourceViewWithReuseAndSyncOnMountBy[A: ClassTag: Reusability](
        streamResource: CtxFn[StreamResource[A]]
      )(implicit
        step:           Step
      ): step.Next[SyncValue[Pot[ReuseView[A]]]] =
        useStreamResourceViewWithReuseAndSyncOnMountBy(step.squash(streamResource)(_))

      // END "BY" "ON MOUNT" METHODS

      //// END STREAM RESOURCE METHODS
    }
  }

  trait HooksApiExt {
    import HooksApiExt._

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

  object implicits extends HooksApiExt
}
