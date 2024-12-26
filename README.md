# Crystal

[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org) ![Build Status](https://github.com/rpiaggio/crystal/workflows/build/badge.svg) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.rpiaggio/crystal_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.rpiaggio/crystal_3)

`crystal` is a toolbelt to help build reactive UI apps in Scala by providing:

- A structure for managing delayed values (`Pot`, `PotOption`).
- Wrappers for values derived from state with a callback function to modify them (`View`, `ViewOpt`, `ViewList`).
- A way to delegate reusability to another type (`Reuse`). Useful for types for which universal reusability cannot be defined (like functions or `VdomNode`).
- Tight integration between `scalajs-react` and `cats-effect` + `fs2.Stream`s via hooks.

`crystal` assumes you use a `scalajs-react`'s `core-bundle-cb_io`, where the default sync effect is `CallbackTo` and the default async effect is `IO`. However, the library should compile with another bundle.

# Core

## Pot[A]

A `Pot[A]` represents a value of type `A` that has been requested somewhere and may or not be available yet, or the request may have failed.

It is a sum type consisting of:
- `Pending`.
- `Ready(<A>)`.
- `Error(<Throwable>)`.

The `crystal.implicits.*` import will provide:
- Instances for `cats` `MonadError`, `Traverse`, `Align` and `Eq` (as long as there's an `Eq[A]` in scope).
- Convenience extension methods: `<Any>.ready`, `<Option[A]>.toPot`, `<Try[A]>.toPot` and `<Option[Try[A]]>.toPot`.

The `crystal.react.implicits.*` import will provide:
- `Reusability[Pot[A]]` (as long as there's a `Reusability[A]` in scope).
- Extesion methods:
  - `renderPending(f: => VdomNode): VdomNode`
  - `renderError(f: Throwable => VdomNode): VdomNode`
  - `renderReady(f: A => VdomNode): VdomNode`

## PotOption[A]

Similar to `Pot[A]` but provides one additinal state. Its values can be:
- `Pending`.
- `ReadyNone`.
- `ReadySome(<A>)`.
- `Error(<Throwable>)`.

It is useful in some situations (see `Hooks` below).

# scalajs-react

## View[A]

A `View[A]` wraps a value of type `A` and a callback to modify it effectfully: `(A => A) => Callback`.

It is useful for passing state down the component hierarchy, allowing descendants to modify it.

It provides several `zoom` functions for drilling down its properties. It also allows effects to be chained whenever it's modified (`withOnMod`).

`ViewOpt[A]` and `ViewList[A]` are variants that hold a value known to be an `Option[A]` or `List[A]` respectively. They are returned when `zoom`ing using `Optional`, `Prism` or `Traversal`.

## ViewThrottler[A]

A `ViewThrowttler[A]` creates two `View[A]` from a given `View[A]`:
  - A `throttledView`, which can be paused. While paused, it accumulates updates and applies them all at once upon timeout. The callback is called only once and only to the function provided in the last update during the pause. If unpaused, it acts as a normal `ViewF`.
  - A `throttlerView`, which will pause the `throttledView` whenever it is modified.

 This is particularly useful for values that can be both updated from a UI and from a server. The `throttlerView` should be used in the UI, while the `throttledView` should be used for the server updates. This way, the server updates will pause whenever the user changes a value. If the server sends updates for every changed value, the throttling will avoid the UI from glitching between old and new values when the UI is updated quickly.

## Reuse[A]

A `Reuse[A]` wraps a value of type `A` and a hidden value of another type `B` such that there is a implicit `Reusability[B]`.

The instance of `Reuse[A]` will be reused as long as the associated value `B` can be reused.

This is useful to define `Reusability` for types where universal reusability can't be defined. For example, we could define reusability for a function `(S, T) => U` can be turned into a `Reuse[T => U]` by specifying a curried value of `S` (and assuming there's a `Reusability[S]` in scope).

## Hooks

### useSingleEffect

Provides a context in which to run a single effect at a time.

When a new effect is submitted, the previous one is canceled. Also cancels the effect on unmount.

A submitted effect can be explicitly canceled too.

If a `debounce` is passed, the hooks guarantees that effect invocations are spaced at least by the specified duration.

``` scala
  useSingleEffect(): Reusable[UseSingleEffect]

  useSingleEffect(debounce: FiniteDuration): Reusable[UseSingleEffect]

  useSingleEffectBy(debounce: Ctx => FiniteDuration): Reusable[UseSingleEffect]
```

where

``` scala
trait UseSingleEffect:
  def submit(effect: IO[Unit]): IO[Unit]
  val cancel: IO[Unit]
```

#### Example:
``` scala
ScalaFnComponent
  .withHooks[Props]
  ...
  .useSingleEffect(1.second)
  .useEffectWithDepsBy( ... => deps)( (..., singleEffect) => deps => singleEffect.submit(longRunningEffect) )
  // Previous `longRunningEffect` is cancelled immediately and new one is ran after 1 second
```

### useShadowRef

When passed a value, this hooks keeps the value in a ref with the latest value, and returning a read-only ref.

This is useful when a stable callback (as those created by `useCallback`) wants to access a value that can change, but we don't want to redefine the callback in each render.

``` scala
  useShadowRef[A](value: Ctx => A): NonEmptyRef.Get[A]
```

### useStateCallback

Class components allow us to specify a callback when we modify state. The callback is executed when state is modified and is passed the new state value.

This is not available in functional components. This hook seeks to emulate such functionality.

Given a state created with `.useState`, the hook allows us to register a callback that will be ran once, the next time the state changes. The callback will be passed the new state value.

``` scala
  useStateCallbackBy[A](state: Ctx => Hooks.UseState[A]): (A => Callback) => Callback
```

#### Example:
``` scala
ScalaFnComponent
  .withHooks[Props]
  ...
  .useState(SomeValue)
  .useStateCallbackBy( (..., state) => state)
  .useEffectBy( (..., state, stateCallback) => 
    state.modState(...) >> stateCallback(value => effect(value))
  ) // `effect` is run with new `value`, after `modState` completes.
```

### useStateView

Provides state as a `View`.

Functionally equivalent to `useState` but the `View` is more practical to pass around to child components, zoom into members and define callbacks upon state change.

``` scala
  useStateView[A](initialValue: => A): View[A]

  useStateViewBy[A](initialValue: Ctx => A): View[A]
```

### useStateViewWithReuse

Similar to `useStateView` but returns a `Reuse[View[A]]`. The resulting `View` is reused by its value and thus requires an implicit `Reusability[A]`, as well as a `ClassTag[A]`.

``` scala
  useStateViewWithReuse[A: ClassTag: Reusability](initialValue: => A): Reuse[View[A]]

  useStateViewWithReuseBy[A: ClassTag: Reusability](initialValue: Ctx => A): Reuse[View[A]]
```

### useThrottlingStateView

Same as `useStateView` but provides 2 `View`s over the same value, See [`ViewThrottler[A]`](#viewthrottlera).

### useSerialState

Creates component state that is reused as long as it's not updated.

``` scala
  useSerialState[A](initialValue: => A): UseSerialState[A]

  useSerialStateBy[A](initialValue: Ctx => A): UseSerialState[A]
```

where

``` scala
trait UseSerialState[A]:
  val value: Reusable[A]
  val setState: Reusable[A => Callback]
  val modState: Reusable[(A => A) => Callback]
```

Reusability of `UseSerialState[A]` and its members depends on an internal counter which is updated every time the wrapped value changes. This is useful to provide stable reusability to types where we don't have or can't define `Reusability` instances.

Usage is the same as for `.useState`/`.useStateBy`.

### useSerialStateView

Version of `useSerialState` that returns a `Reuse[View[A]]`.

``` scala
  useSerialStateView[A](initialValue: => A): Reuse[View[A]]

  useSerialStateViewBy[A](initialValue: Ctx => A): Reuse[View[A]]
```

### useEffectWhenDepsReady

Version of `useEffect` applicable only when there's a unique dependency of type `Pot[A]`. It will trigger the effect only when the dependency transitions to a `Ready` state, whether it was from `Pending` or `Error`. There are also `WhenDepsReadyOrChange` versions, which will also trigger if the dependencies change value once in `Ready` state.

Note that multiple `Pot` dependencies can be combined into one with `.tupled`.

``` scala
  useEffectWhenDepsReady[D](deps: => Pot[D])(effect: D => Callback)
  useEffectWhenDepsReadyBy[D](deps: Ctx => Pot[D])(effect: Ctx => D => Callback)

  useEffectWhenDepsReadyOrChange[D: Reusability](deps: => Pot[D])(effect: D => Callback)
  useEffectWhenDepsReadyOrChangeBy[D: Reusability](deps: Ctx => Pot[D])(effect: Ctx => D => Callback)

  useEffectWhenDepsReady[D](deps: => Pot[D])(effect: D => IO[Unit])
  useEffectWhenDepsReadyBy[D](deps: Ctx => Pot[D])(effect: Ctx => D => IO[Unit])

  useEffectWhenDepsReadyOrChange[D: Reusability](deps: => Pot[D])(effect: D => IO[Unit])
  useEffectWhenDepsReadyOrChangeBy[D: Reusability](deps: Ctx => Pot[D])(effect: Ctx => D => IO[Unit])

  useEffectWhenDepsReady[D](deps: => Pot[D])(effect: D => CallbackTo[Callback]) // return cleanup
  useEffectWhenDepsReadyBy[D](deps: Ctx => Pot[D])(effect: Ctx =>D => CallbackTo[Callback]) // return cleanup

  useEffectWhenDepsReadyOrChange[D: Reusability](deps: => Pot[D])(effect: D => CallbackTo[Callback]) // return cleanup
  useEffectWhenDepsReadyOrChangeBy[D: Reusability](deps: Ctx => Pot[D])(effect: Ctx =>D => CallbackTo[Callback]) // return cleanup
```

### useAsyncEffect

Version of `useEffect` that avoids race conditions when executing async effects. This is achieved by cancelling the previous instance of the effect before executing a new one.

Also allows returning a cleanup effect, which `useEffect` only supports when used with the default sync effect (usually `CallbackTo`).

``` scala
  useAsyncEffect(effect: IO[Unit])
  useAsyncEffect(effect: IO[IO[Unit]])  // return a cleanup effect
  useAsyncEffectBy(effect: Ctx => IO[Unit])
  useAsyncEffectBy(effect: Ctx => IO[IO[Unit]]) // return a cleanup effect

  useAsyncEffectWithDeps[D: Reusability](deps: => D)(effect: D => IO[Unit])
  useAsyncEffectWithDeps[D: Reusability](deps: => D)(effect: D => IO[IO[Unit]]) // return a cleanup effect
  useAsyncEffectWithDepsBy[D: Reusability](deps: Ctx => D)(effect: Ctx => D => IO[Unit])
  useAsyncEffectWithDepsBy[D: Reusability](deps: Ctx => D)(effect: Ctx => D => IO[IO[Unit]]) // return a cleanup effect

  useAsyncEffectOnMount(effect: IO[IO[Unit]])
  useAsyncEffectOnMount(effect: IO[Unit]) // return a cleanup effect
  useAsyncEffectOnMountBy(effect: Ctx => IO[Unit])
  useAsyncEffectOnMountBy(effect: Ctx => IO[IO[Unit]]) // return a cleanup effect

  useAsyncEffectWhenDepsReady[D](deps: => Pot[D])(effect: D => IO[Unit])
  useAsyncEffectWhenDepsReady[D](deps: => Pot[D])(effect: D => IO[IO[Unit]]) // return a cleanup effect
  useAsyncEffectWhenDepsReadyBy[D](deps: Ctx => Pot[D])(effect: Ctx => D => IO[Unit])
  useAsyncEffectWhenDepsReadyBy[D](deps: Ctx => Pot[D])(effect: Ctx => D => IO[IO[Unit]]) // return a cleanup effect

  useAsyncEffectWhenDepsReadyOrChange[D: Reusability](deps: => Pot[D])(effect: D => IO[Unit])
  useAsyncEffectWhenDepsReadyOrChange[D: Reusability](deps: => Pot[D])(effect: D => IO[IO[Unit]]) // return a cleanup effect
  useAsyncEffectWhenDepsReadyOrChangeBy[D: Reusability](deps: Ctx => Pot[D])(effect: Ctx => D => IO[Unit])
  useAsyncEffectWhenDepsReadyOrChangeBy[D: Reusability](deps: Ctx => Pot[D])(effect: Ctx => D => IO[IO[Unit]]) // return a cleanup effect
```


### useEffectResult

Stores the result `A` of an effect in state. The state is provided as `Pot[A]`, with value `Pending` until the effect completes (and `Error` if it fails).

Note that all versions either have dependencies or are executed `onMount`. It doesn't make sense to execute the effect on each render since its completion will alter state and force a rerender, which would provoke a render loop. The naming keeps the `WithDeps`, even though it's redundant, for consistency with the `useEffect` family of hooks.

Also note that when dependencies change, the hook value will revert to `Pending` until the new effect completes. If this is undesireable, there are `useEffectKeepResult*` variants which will instead keep the hook value as `Ready(oldValue)` until the new effect completes.

There are also `WhenDepsReady` versions, which will only execute the effect when dependencies transition to `Ready`. If they transition to `Pending` or `Error`, then the result will revert to `Pending`. However, if the `KeepResult` version is used, it will retain the last value.

Furthermore, there are also `WhenDepsReadyOrChange` versions, which will only execute the effect when dependencies transition to `Ready` or change value once `Ready`. If they change or transition to `Pending` or `Error`, then the result will revert to `Pending`. However, if the `KeepResult` version is used, it will retain the last value.


``` scala
  useEffectResultWithDeps[D: Reusability, A](deps: => D)(effect: D => IO[A]): Pot[A]
  useEffectResultWithDepsBy[D: Reusability, A](deps: Ctx => D)(effect: Ctx => D => IO[A]): Pot[A]

  useEffectKeepResultWithDeps[D: Reusability, A](deps: => D)(effect: D => IO[A]): Pot[A]
  useEffectKeepResultWithDepsBy[D: Reusability, A](deps: Ctx => D)(effect: Ctx => D => IO[A]): Pot[A]

  useEffectResultOnMount[A](effect: IO[A]): Pot[A]
  useEffectResultOnMountBy[A](effect: Ctx => IO[A]): Pot[A]

  useEffectResultWhenDepsReady[D: Reusability, A](deps: => D)(effect: D => IO[A]): Pot[A]
  useEffectResultWhenDepsReadyBy[D: Reusability, A](deps: Ctx => D)(effect: Ctx => D => IO[A]): Pot[A]

  useEffectResultWhenDepsReadyOrChange[D: Reusability, A](deps: => D)(effect: D => IO[A]): Pot[A]
  useEffectResultWhenDepsReadyOrChangeBy[D: Reusability, A](deps: Ctx => D)(effect: Ctx => D => IO[A]): Pot[A]

  useEffectKeepResultWhenDepsReady[D: Reusability, A](deps: => D)(effect: D => IO[A]): Pot[A]
  useEffectKeepResultWhenDepsReadyBy[D: Reusability, A](deps: Ctx => D)(effect: Ctx => D => IO[A]): Pot[A]

  useEffectKeepResultWhenDepsReadyOrChange[D: Reusability, A](deps: => D)(effect: D => IO[A]): Pot[A]
  useEffectKeepResultWhenDepsReadyOrChangeBy[D: Reusability, A](deps: Ctx => D)(effect: Ctx => D => IO[A]): Pot[A]
```

#### Example:
``` scala
ScalaFnComponent
  .withHooks[Props]
  ...
  .useEffectResultOnMount(UUIDGen.randomUUID)
  .render( (..., uuidPot) => 
    uuidPot.fold(
      "Pending...",
      t => s"Error! ${e.getMessage}",
      uuid => s"Your fresh UUID: $uuid"
    )
  )
```

### useResource

Opens a `Resource[IO, A]` upon mount or dependency change, and provides its value as a `Pot[A]`.

The resource is gracefully closed upon unmount or dependency change.

Note that all versions either have dependencies or are executed `onMount`. It doesn't make sense to open a resource on each render since once the resource is acquired it will alter state and force a rerender, which would provoke a render loop.

``` scala
  useResource[D: Reusability, A](deps: => D)(resource: D => Resource[IO, A]): Pot[A]
  useResourceBy[D: Reusability, A](deps: Ctx => D)(resource: Ctx => D => Resource[IO, A]): Pot[A]

  useResourceOnMount[A](resource: Resource[IO, A]): Pot[A]
  useResourceOnMountBy[A](resource: Ctx => Resource[IO, A]): Pot[A]
```

### useStream

Executes and drains a `fs2.Stream[IO, A]` upon mount or dependency change, and provides the latest value from the stream as a `PotOption[A]`.

The fiber evaluating the stream is canceled upon unmount or dependency change.

Note that all versions either have dependencies or are executed `onMount`. It doesn't make sense to open a stream on each render since executing the stream will alter state and force a rerender, which would provoke a render loop.

``` scala
  useStream[D: Reusability, A](deps: => D)(stream: D => fs2.Stream[IO, A]): PotOption[A]
  useStreamBy[D: Reusability, A](deps: Ctx => D)(stream: Ctx => D => fs2.Stream[IO, A]): PotOption[A]

  useStreamOnMount[A](stream: fs2.Stream[IO, A]): PotOption[A]
  useStreamOnMountBy[A](stream: Ctx => fs2.Stream[IO, A]): PotOption[A]
```

The resulting `PotOption[A]` takes one of these values:
- `Pending`: Fiber hasn't started yet
- `ReadyNone`: Fiber has started but no value has been produced by the stream yet.
- `ReadySome(a)`: `a` is the last value produced by the stream.
- `Error(t)`: Fiber raised an exception `t`.

### useStreamView

Like `useStream` but returns a `PotOption[View[A]]`, allowing local modifications to the state once it's `ReadySome`.

In other words, the state will be modified on every new value produced by the stream, and also on every invocation to `set` or `mod` on the `View`.

``` scala
  useStreamView[D: Reusability, A](deps: => D)(stream: D => fs2.Stream[IO, A]): PotOption[View[A]]
  useStreamViewBy[D: Reusability, A](deps: Ctx => D)(stream: Ctx => D => fs2.Stream[IO, A]): PotOption[View[A]]

  useStreamViewOnMount[A](stream: fs2.Stream[IO, A]): PotOption[View[A]]
  useStreamViewOnMountBy[A](stream: Ctx => fs2.Stream[IO, A]): PotOption[View[A]]
```

### useStreamResource

Given a `Resource[IO, fs2.Stream[IO, A]]`, combines `useResource` and `useStream` on it.

In other words, when mounting or depedency change, the resource is allocated and the resulting stream starts being evaluated.

Upon unmount or dependency change, the evaluating fiber is cancelled and the resource closed.

The resource is also closed if the stream terminates.

``` scala
  useStreamResource[D: Reusability, A](deps: => D)(streamResource: D => Resource[IO, fs2.Stream[IO, A]]): PotOption[A]
  useStreamResourceBy[D: Reusability, A](deps: Ctx => D)(streamResource: Ctx => D => Resource[IO, fs2.Stream[IO, A]]): PotOption[A]

  useStreamResourceOnMount[A](streamResource: Resource[IO, fs2.Stream[IO, A]]): PotOption[A]
  useStreamResourceOnMountBy[A](streamResource: Ctx => Resource[IO, fs2.Stream[IO, A]]): PotOption[A]
```

### useStreamResourceView

Given a `Resource[IO, fs2.Stream[IO, A]]`, combines `useResource` and `useStreamView` on it.

Like `useStreamResource` but returns a `PotOption[View[A]]`, allowing local modifications to the state once it's `Ready`.

``` scala
  useStreamResourceView[D: Reusability, A](deps: => D)(streamResource: D => Resource[IO, fs2.Stream[IO, A]]): PotOption[View[A]]
  useStreamResourceViewBy[D: Reusability, A](deps: Ctx => D)(streamResource: Ctx => D => Resource[IO, fs2.Stream[IO, A]]): PotOption[View[A]]

  useStreamResourceViewOnMount[A](streamResource: Resource[IO, fs2.Stream[IO, A]]): PotOption[View[A]]
  useStreamResourceViewOnMountBy[A](streamResource: Ctx => Resource[IO, fs2.Stream[IO, A]]): PotOption[View[A]]
```

### useEffectStream

Executes and drains a `fs2.Stream[IO, Unit]` upon mount or dependency change. If still running, execution is cancelled upon unmount or dependency change.

Like the `useEffect` family of hooks, this hook doesn't add any new parameters to the context.

``` scala
  useEffectStream(stream: D => fs2.Stream[IO, Unit])
  useEffectStreamBy(stream: Ctx => fs2.Stream[IO, Unit])

  useEffectStreamWithDeps[D: Reusability](deps: => D)(stream: D => fs2.Stream[IO, Unit])
  useEffectStreamWithDepsBy[D: Reusability](deps: Ctx => D)(stream: Ctx => D => fs2.Stream[IO, Unit])

  useEffectStreamOnMount(stream: fs2.Stream[IO, Unit])
  useEffectStreamOnMountBy(stream: Ctx => fs2.Stream[IO, Unit])

  useEffectStreamWhenDepsReady[D](deps: => Pot[D])(stream: D => fs2.Stream[IO, Unit])
  useEffectStreamWhenDepsReadyBy[D](deps: Ctx => Pot[D])(stream: Ctx => D => fs2.Stream[IO, Unit])
```

### useEffectStreamResource

Given a `Resource[IO, fs2.Stream[IO, Unit]]`, opens the resource and executes and drains the stream upon mount or dependency change. If still running, execution is cancelled and the resource closed upon unmount or dependency change. The resource is also closed if the stream terminates.

Like the `useEffect` family of hooks, this hook doesn't add any new parameters to the context.

``` scala
  useEffectStreamResource(stream: Resource[IO, fs2.Stream[IO, Unit]])
  useEffectStreamResourceBy(stream: Ctx => Resource[IO, fs2.Stream[IO, Unit]])

  useEffectStreamResourceWithDeps[D: Reusability](deps: => D)(stream: D => Resource[IO, fs2.Stream[IO, Unit]])
  useEffectStreamResourceWithDepsBy[D: Reusability](deps: Ctx => D)(stream: Ctx => D => Resource[IO, fs2.Stream[IO, Unit]])

  useEffectStreamResourceOnMount(stream: Resource[IO, fs2.Stream[IO, Unit]])
  useEffectStreamResourceOnMountBy(stream: Ctx => Resource[IO, fs2.Stream[IO, Unit]])

  useEffectStreamResourceWhenDepsReady[D](deps: => Pot[D])(stream: D => Resource[IO, fs2.Stream[IO, Unit]])
  useEffectStreamResourceWhenDepsReadyBy[D](deps: Ctx => Pot[D])(stream: Ctx => D => Resource[IO, fs2.Stream[IO, Unit]])
```

### `useSignalStream` / `useSignalStreamByReuse`

Given a value, creates an `fs2.Stream` that will emit a new value every time the value changes. Equality is tested by `Eq`/`Reusability`. The stream is created when the component is mounted and memoized and terminates when the component unmounts.

``` scala
  useSignalStream[A: Eq](value: A): Reusable[Pot[fs2.Stream[DefaultA, A]]]
  useSignalStreamByReuse[A: Reusability](value: A): Reusable[Pot[fs2.Stream[DefaultA, A]]]
```

### `scalajs-react` <-> `cats-effect` interop

The `crystal.react.implicits.*` import will provide the following methods:

#### Effect conversion

- `<CallbackTo[A]>.to[F]: F[A]` - converts a `CallbackTo` to the effect `F`. `<Callback>.to[F]` returns `F[Unit]`. (Requires implicit `Sync[F]`).
- `<F[A]>.runAsync(cb: Either[Throwable, A] => F[Unit]): Callback` - When the resulting `Callback` is run, `F[A]` will be run asynchronously and its result will be handled by `cb`. (Requires implicit `Dispatcher[F]`).
- `<F[A]>.runAsyncAndThen(cb: Either[Throwable, A] => Callback): Callback` - When the resulting `Callback` is run, `F[A]` will be run asynchronously and its result will be handled by `cb`. The difference with `runAsyncCB` is that the result handler returns a `Callback` instead of `F[A]`. (Requires implicit `Dispatcher[F]`).
- `<F[A]>.runAsyncAndForget: Callback` - When the resulting `Callback` is run, `F[A]` will be run asynchronously and its result will be ignored, as well as any errors it may raise. (Requires implicit `Dispatcher[F]`).
- `<F[Unit]>.runAsyncAndThen(cb: Callback, errorMsg: String?): Callback` - When the resulting `Callback` is run, `F[Unit]` will be run asynchronously. If it succeeds, then `cb` will be run. If it fails, `errorMsg` will be logged. (Requires implicit `Dispatcher[F]` and `Logger[F]`).
- `<F[Unit]>.runAsync(errorMsg: String?): Callback` - When the resulting `Callback` is run, `F[Unit]` will be run asynchronously. If it fails, `errorMsg` will be logged. (Requires implicit `Dispatcher[F]` and `Logger[F]`).

Please note that in all cases the the `Callback` returned by `.runAsync*` will complete immediately.

#### Extensions to `BackendScope`

- `<BackendScope[P, S]>.propsIn[F]: F[P]` - (Requires implicit `Sync[F]`).
- `<BackendScope[P, S]>.stateIn[F]: F[S]` - (Requires implicit `Sync[F]`),
- `<BackendScope[P, S]>.setStateIn[F](s: S): F[Unit]` - will complete once the state has been set. Therefore, use this instead of `<BackendScope[P, S]>.setState.to[F]`, which would complete immediately. (Requires implicit `Async[F]`).
- `<BackendScope[P, S]>.modStateIn[F](f: S => S): F[Unit]` - same as above. (Requires implicit `Async[F]`).
- `<BackendScope[P, S]>.modStateWithPropsIn[F](f: (S, P) => S): F[Unit]` - (Requires implicit `Async[F]`).
