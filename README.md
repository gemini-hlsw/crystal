# Crystal

[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org) ![Build Status](https://github.com/rpiaggio/crystal/workflows/build/badge.svg) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.rpiaggio/crystal_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.rpiaggio/crystal_2.13)

`crystal` is a toolbelt to help build reactive UI apps in Scala by providing:
* Utilities for initializing and accessing global immutable context (`AppRootContext`).
* A structure for managing server roundtrips (`Pot`).
* Wrappers for values derived from state with a callback function to modify them (`ViewF`, `ViewOptF`, `ViewListF`).

Additionally, for `scalajs-react` apps it provides:
* A root component to hold the application state (`AppRoot`).
* A component that dynamically renders values from `fs2.Stream`s (`StreamRenderer`).
* A component that dynamically renders values from `fs2.Stream`s, allowing children to modify the value too (`StreamRendererMod`).
* Conversions between `Callback`s and `cats-effect` effects (`implicits._`).
* `Reusability` and other utilities for `Pot` and `View*F` (`implicits._`)

The library takes a tagless approach based on `cats-effect`. Logging is performed via `log4cats`.

## Core

### Pot[A]

A `Pot[A]` represents a value of type `A` that has been requested somewhere and may or not be available yet, or the request may have failed.

It is a sum type consisting of:
- `Pending(<Long>)`, where the `Long` is meant to store the instant of creation, in millis since epoch. It is initialized to `System.currentTimeMillis()` by default.
- `Ready(<A>)`.
- `Error(<Throwable>)`.

`Pot[A]` implements the following methods: 
* `map[B](f: A => B): Pot[B]`
* `fold[B](fp: Long => B, fe: Throwable => B, fr: A => B): B`
* `flatten[B]: Pot[B]` (if `A <: Pot[B]`)
* `flatMap[B](f: A => Pot[B]): Pot[B]`
* `toOption: Option[A]`
* `toTryOption: Option[Try[A]]`

The `crystal.implicits._` import will provide:
* Instances for `cats` `MonadError`, `Traverse`, `Align` and `Eq` (as long as there's an `Eq[A]` in scope).
* Convenience methods: `<Any>.ready`, `<Option[A]>.toPot`, `<Try[A]>.toPot` and `<Option[Try[A]]>.toPot`.

The `crystal.react.implicits._` import will provide:
* `Reusability[Pot[A]]` (as long as there's a `Reusability[A]` in scope).
* Convenience methods:
  * `renderPending(f: Long => VdomNode): VdomNode`
  * `renderError(f: Throwable => VdomNode): VdomNode`
  * `renderReady(f: A => VdomNode): VdomNode`

### ViewF[F, A]

A `ViewF[F, A]` wraps a value of type `A` and a callback to modify it effectfully: `(A => A) => F[Unit]`.

It is useful for passing state down the component hierarchy, allowing desdendents to modify it.

Provides the following methods:
* `get: A` - returns the wrapped `A`.
* `mod(f: A => A): F[Unit]` - returns the effect for modifying the state using `f`.
* `set(a: A): F[Unit]` = `mod(_ => a)`.
* `modAndGet(f: A => A): F[A]` - same as `mod(f)` but returns the modified `A`.
* `withOnMod(f: A => F[Unit])` - creates a new `ViewF` that chains the passed effect whenever `mod` (or `set`) is called.
* `zoom` methods - Create a new `ViewF` focused on a part of `A`. This method can take either raw getter and setter functions or a `monocle` `Lens`, `Optional`, `Prism` or `Traversal`.

`ViewOptF[F, A]` and `ViewListF[F, A]` are variants that hold a value known to be an `Option[A]` or `List[A]` respectively. They are returned when `zoom`ing using `Optional`, `Prism` or `Traversal`.

Requires the following implicits in scope:
* `Async[F]`
* `ContextShift[F]`

The `crystal.react.implicits._` import will provide:
* `Reusability[ViewF[F, A]]`, `Reusability[ViewOptF[F, A]]` and `Reusability[ViewListF[F, A]]`, based solely on the wrapped value `A` (and as long as there's a `Reusability[A]` in scope).

## scalajs-react

### StreamRenderer

`StreamRenderer.build(fs2.Stream[F, A])` will create a component that takes a rendering function `Pot[A] => VdomNode` as properties.

The component will keep a `Pot[A]` as state, and will use the rendering function to render such state.

State initialized to `Pending` upon mounting; and then set to `Ready(<A>)` with each element received in the `Stream`, or `Error(<Throwable>)` if the `Stream` fails.

You should store the component (in a `Backend` or in `State` for example) and reuse it. Do not `build` it on each `render`.

Requires the following implicits in scope:
* `ConcurrentEffect[F]`
* `Logger[F]`
* `Reusability[A]`
* (Optionally) `Reusability[Pot[A] => VdomNode]`. If not provided, the render function will never be reused.

### StreamRendererMod

Same as `StreamRenderer` but allows the rendering function to modify the state. Therefore, the rendering function will be a `Pot[ViewF[F, A]] => VdomNode` instead.

This is useful, for example, when subscribing to a stream from a server and children can invoke mutations in the server. Children can chain the `ViewF`'s `mod/set` effect to the invocation of the mutation in order to display the change immediately instead of having to wait for the roundtrip to the server.

When the value is modified by children in such a way, values from the stream will not be propagated during a period of time. Otherwise, this would caulse flicker in the UI if the children modify the value repeatedly in a short period of time, as the intermediate values are received in the stream. By default, the hold period is `2 seconds`, but it can be modified by passing a 2nd parameter `holdAfterMod` to `StreamRendererMod.build`.

### `scalajs-react` <-> `cats-effect` interop

The `crystal.react.implicits._` import will provide the following methods:
* `<CallbackTo[A]>.to[F]: F[A]` - converts a `CallbackTo` to the effect `F`. `<Callback>.to[F]` returns `F[Unit]`. (Requires implicit `Sync[F]`).
* `<BackendScope[P, S]>.propsIn[F]: F[P]` - (Requires implicit `Sync[F]`).
* `<BackendScope[P, S]>.stateIn[F]: F[S]` - (Requires implicit `Sync[F]`),
* `<BackendScope[P, S]>.setStateIn[F](s: S): F[Unit]` - will complete once the state has been set. Therefore, use this instead of `<BackendScope[P, S]>.setState.to[F]`, which would complete immediately. (Requires implicit `Async[F]`).
* `<BackendScope[P, S]>.modStateIn[F](f: S => S): F[Unit]` - same as above. (Requires implicit `Async[F]`).
* `<BackendScope[P, S]>.modStateWithPropsIn[F](f: (S, P) => S): F[Unit]` - (Requires implicit `Async[F]`).
* `<SyncIO[A]>.toCB: CallbackTo[A]` - converts a `SyncIO` to `CallbackTo`.
* `<F[A]>.runInCBAndThen(cb: A => Callback): Callback` - When the resulting `Callback` is run, `F[A]` will be run asynchronously and its value passed to `cb`, whose returned `Callback` will be run then. (Requires implicit `Effect[F]`).
* `<F[A]>.runInCBAndForget(): Callback` - When the resulting `Callback` is run, `F[A]` will be run asynchronously and its value discarded. (Requires implicit `Effect[F]`).
* `<F[Unit]>.runInCBAndThen(cb: Callback): Callback` - When the resulting `Callback` is run, `F[Unit]` will be run asynchronously and when it completes, then `cb` will be run. (Requires implicit `Effect[F]`).
* `<F[Unit]>.runInCB: Callback` - When the resulting `Callback` is run, `F[Unit]` will be run asynchronously. (Requires implicit `Effect[F]`). Please note that the `Callback` will complete immediately.
