// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Resource
import cats.syntax.all.*
import crystal.*
import crystal.react.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA

object UseEffectStreamResource {

  protected def hook[D: Reusability] = CustomHook[WithDeps[D, StreamResource[Unit]]]
    .useAsyncEffectWithDepsBy(props => props.deps): props =>
      deps =>
        val executingResource: Resource[DefaultA, Unit] =
          props
            .fromDeps(deps)
            .flatMap: stream =>
              Resource.make(stream.compile.drain.start)(_.cancel).as(())
        executingResource.allocated.map(_._2) // open the resource and return a close callback
    .build

  object HooksApiExt {
    sealed class Primary[Ctx, Step <: HooksApi.AbstractStep](api: HooksApi.Primary[Ctx, Step]) {

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on each render. If there was another
       * fiber executing from the previous render, it will be cancelled.
       */
      final def useEffectStream(stream: fs2.Stream[DefaultA, Unit])(using step: Step): step.Self =
        useEffectStreamBy(_ => stream)

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on mount or when deps change.The
       * fiber will be cancelled on unmount or deps change.
       */
      final def useEffectStreamWithDeps[D: Reusability](deps: => D)(
        stream: D => fs2.Stream[DefaultA, Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceWithDeps(deps)(deps => Resource.pure(stream(deps)))

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on mount. The fiber will be cancelled
       * on unmount.
       */
      final def useEffectStreamOnMount(
        stream: fs2.Stream[DefaultA, Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceOnMount(Resource.pure(stream))

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber when a `Pot` dependency becomes
       * `Ready`. The fiber will be cancelled on unmount or if the dependency transitions to
       * `Pending` or `Error`.
       */
      final def useEffectStreamWhenDepsReady[D](
        deps: => Pot[D]
      )(stream: D => fs2.Stream[DefaultA, Unit])(using
        step: Step
      ): step.Self =
        useEffectStreamWithDeps(deps.toOption.void)(_ => deps.toOption.map(stream).orEmpty)

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on each render. If there was another
       * fiber executing from the previous render, it will be cancelled.
       */
      final def useEffectStreamBy(stream: Ctx => fs2.Stream[DefaultA, Unit])(using
        step: Step
      ): step.Self =
        useEffectStreamWithDepsBy(_ => NeverReuse)(ctx => _ => stream(ctx))

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on mount or when deps change.The
       * fiber will be cancelled on unmount or deps change.
       */
      final def useEffectStreamWithDepsBy[D: Reusability](deps: Ctx => D)(
        stream: Ctx => D => fs2.Stream[DefaultA, Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceWithDepsBy(deps)(ctx => deps => Resource.pure(stream(ctx)(deps)))

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on mount. The fiber will be cancelled
       * on unmount.
       */
      final def useEffectStreamOnMountBy(
        stream: Ctx => fs2.Stream[DefaultA, Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceOnMountBy(ctx => Resource.pure(stream(ctx)))

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber when a `Pot` dependency becomes
       * `Ready`. The fiber will be cancelled on unmount or if the dependency transitions to
       * `Pending` or `Error`.
       */
      final def useEffectStreamWhenDepsReadyBy[D](
        deps: Ctx => Pot[D]
      )(stream: Ctx => D => fs2.Stream[DefaultA, Unit])(using
        step: Step
      ): step.Self =
        useEffectStreamWithDepsBy(deps(_).toOption.void)(ctx =>
          _ => deps(ctx).toOption.map(stream(ctx)).orEmpty
        )

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on each render, and drain the stream by
       * creating a fiber. If there was another fiber executing from the previous render, it will be
       * cancelled and its resource closed.
       */
      final def useEffectStreamResource(streamResource: StreamResource[Unit])(using
        step: Step
      ): step.Self =
        useEffectStreamResourceBy(_ => streamResource)

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. The fiber will be cancelled and the resource closed
       * on unmount or deps change.
       */
      final def useEffectStreamResourceWithDeps[D: Reusability](deps: => D)(
        streamResource: D => StreamResource[Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceWithDepsBy(_ => deps)(_ => streamResource)

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on mount, and drain the stream by creating
       * a fiber. The fiber will be cancelled and the resource closed on unmount.
       */
      final def useEffectStreamResourceOnMount(
        streamResource: StreamResource[Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceOnMountBy(_ => streamResource)

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` when a `Pot` dependency becomes `Ready`,
       * and drain the stream by creating a fiber. The fiber will be cancelled and the resource
       * closed on unmount or if the dependency transitions to `Pending` or `Error`.
       */
      final def useEffectStreamResourceWhenDepsReady[D](deps: => Pot[D])(
        streamResource: D => StreamResource[Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceWithDeps(deps.toOption.void)(_ =>
          deps.toOption.map(streamResource).orEmpty
        )

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on each render, and drain the stream by
       * creating a fiber. If there was another fiber executing from the previous render, it will be
       * cancelled and its resource closed.
       */
      final def useEffectStreamResourceBy(streamResource: Ctx => StreamResource[Unit])(using
        step: Step
      ): step.Self =
        useEffectStreamResourceWithDepsBy(_ => NeverReuse)(ctx => _ => streamResource(ctx))

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. The fiber will be cancelled and the resource closed
       * on unmount or deps change.
       */
      final def useEffectStreamResourceWithDepsBy[D: Reusability](deps: Ctx => D)(
        streamResource: Ctx => D => StreamResource[Unit]
      )(using step: Step): step.Self =
        api.customBy { ctx =>
          val hookInstance = hook[D]
          hookInstance(WithDeps(deps(ctx), streamResource(ctx)))
        }

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on mount, and drain the stream by creating
       * a fiber. The fiber will be cancelled and the resource closed on unmount.
       */
      final def useEffectStreamResourceOnMountBy(
        streamResource: Ctx => StreamResource[Unit]
      )(using step: Step): step.Self = // () has Reusability = always.
        useEffectStreamResourceWithDepsBy(_ => ())(ctx => _ => streamResource(ctx))

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` when a `Pot` dependency becomes `Ready`,
       * and drain the stream by creating a fiber. The fiber will be cancelled and the resource
       * closed on unmount or if the dependency transitions to `Pending` or `Error`.
       */
      final def useEffectStreamResourceWhenDepsReadyBy[D](deps: Ctx => Pot[D])(
        streamResource: Ctx => D => StreamResource[Unit]
      )(using step: Step): step.Self =
        useEffectStreamResourceWithDepsBy(deps(_).toOption.void)(ctx =>
          _ => deps(ctx).toOption.map(streamResource(ctx)).orEmpty
        )
    }

    final class Secondary[Ctx, CtxFn[_], Step <: HooksApi.SubsequentStep[Ctx, CtxFn]](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ) extends Primary[Ctx, Step](api) {

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on each render. If there was another
       * fiber executing from the previous render, it will be cancelled.
       */
      final def useEffectStreamBy(stream: CtxFn[fs2.Stream[DefaultA, Unit]])(using
        step: Step
      ): step.Self =
        useEffectStreamWithDepsBy(_ => NeverReuse)(ctx => _ => step.squash(stream)(ctx))

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on mount or when deps change.The
       * fiber will be cancelled on unmount or deps change.
       */
      final def useEffectStreamWithDepsBy[D: Reusability](deps: CtxFn[D])(
        stream: CtxFn[D => fs2.Stream[DefaultA, Unit]]
      )(using step: Step): step.Self =
        useEffectStreamResourceWithDepsBy(step.squash(deps)(_))(ctx =>
          deps => Resource.pure(step.squash(stream)(ctx)(deps))
        )

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber on mount. The fiber will be cancelled
       * on unmount.
       */
      final def useEffectStreamOnMountBy(
        stream: CtxFn[fs2.Stream[DefaultA, Unit]]
      )(using step: Step): step.Self =
        useEffectStreamResourceOnMountBy((ctx: Ctx) =>
          Resource.pure[DefaultA, fs2.Stream[DefaultA, Unit]](step.squash(stream)(ctx))
        )

      /**
       * Drain a `fs2.Stream[Async, Unit]` by creating a fiber when a `Pot` dependency becomes
       * `Ready`. The fiber will be cancelled on unmount or if the dependency transitions to
       * `Pending` or `Error`.
       */
      final def useEffectStreamWhenDepsReadyBy[D](
        deps: CtxFn[Pot[D]]
      )(stream: CtxFn[D => fs2.Stream[DefaultA, Unit]])(using
        step: Step
      ): step.Self =
        useEffectStreamWithDepsBy(step.squash(deps)(_).toOption.void)(ctx =>
          _ =>
            step
              .squash(deps)(ctx)
              .toOption
              .map(readyDeps => step.squash(stream)(ctx)(readyDeps))
              .orEmpty
        )

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on mount or when dependencies change, and
       * drain the stream by creating a fiber. The fiber will be cancelled and the resource closed
       * on unmount or deps change.
       */
      final def useEffectStreamResourceWithDepsBy[D: Reusability](deps: CtxFn[D])(
        streamResource: CtxFn[D => StreamResource[Unit]]
      )(using step: Step): step.Self =
        useEffectStreamResourceWithDepsBy(step.squash(deps)(_))(step.squash(streamResource)(_))

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` on mount, and drain the stream by creating
       * a fiber. The fiber will be cancelled and the resource closed on unmount.
       */
      final def useEffectStreamResourceOnMountBy(
        streamResource: CtxFn[StreamResource[Unit]]
      )(using step: Step): step.Self =
        useEffectStreamResourceOnMountBy(step.squash(streamResource)(_))

      /**
       * Open a `Resource[Async, fs.Stream[Async, Unit]]` when a `Pot` dependency becomes `Ready`,
       * and drain the stream by creating a fiber. The fiber will be cancelled and the resource
       * closed on unmount or if the dependency transitions to `Pending` or `Error`.
       */
      final def useEffectStreamResourceWhenDepsReadyBy[D](
        deps: CtxFn[Pot[D]]
      )(stream: CtxFn[D => StreamResource[Unit]])(using
        step: Step
      ): step.Self =
        useEffectStreamResourceWithDepsBy(step.squash(deps)(_).toOption.void)(ctx =>
          _ =>
            step
              .squash(deps)(ctx)
              .toOption
              .map(readyDeps => step.squash(stream)(ctx)(readyDeps))
              .orEmpty
        )
    }
  }

  protected trait HooksApiExt {
    import HooksApiExt.*

    implicit def hooksExtEffectStreamResourceView1[Ctx, Step <: HooksApi.AbstractStep](
      api: HooksApi.Primary[Ctx, Step]
    ): Primary[Ctx, Step] =
      new Primary(api)

    implicit def hooksExtEffectStreamResourceView2[
      Ctx,
      CtxFn[_],
      Step <: HooksApi.SubsequentStep[Ctx, CtxFn]
    ](
      api: HooksApi.Secondary[Ctx, CtxFn, Step]
    ): Secondary[Ctx, CtxFn, Step] =
      new Secondary(api)
  }

  object syntax extends HooksApiExt
}
