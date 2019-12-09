# Crystal

`import crystal._`

Functional, tagless and lens-based, global state management. With `scalajs-react` `fs2.Stream` integrations.

## Core

### Model

Model is just a `case class` with all the client state.

`monocle` lenses are used to read/write to the model. `@Lenses` on Model are recommended.

The model must be placed somewhere widely accessible. Eg: `object AppState`.

It must be defined by calling `Model[F, M](<initial model>)`, where `F[_]` is the effect context to use and `M` is the model type.

Eg, if you choose to use `IO`:
```scala
import scala.concurrent.ExecutionContext.global
import crystal._
import monocle.Lens
import monocle.macros.Lenses

object AppState {
  implicit private val timerIO: Timer[IO] = cats.effect.IO.timer(global)
  implicit private val csIO: ContextShift[IO] = IO.contextShift(global)

  @Lenses
  case class RootModel(
                        todos: Pot[Todos],
                        motd: Pot[String],
                        motdInstant: Option[Instant],
                        progress: Int
                      )

  val rootModel = Model[IO, RootModel](RootModel(Empty, Empty, None, 0))
}
``` 

Note that the chosen effect context `F[_]` must have `cats.effects.{ConcurrentEffect, Timer}` type class instances.

### FixedLens

A `FixedLens[F[_], A]` is just a `monocle.Lens[M, A]` but it's fixed on an underlying instance of `M`.

It's purpose is not to have to pass `M` (the Model type) everywhere.

Also, it provides effectful `get`, `set` and `modify` methods that directly act on the underlying instance of `M`.

### View

A `View[F[_], A]` is simply a partial view of the Model.

It can be created by calling `rootModel.view(<monocle.Lens[M, A]>)`.

It provides:
* A `FixedLens[F, A]` to effectfully read and write to the model (through the provided `Lens`).
* An `fs.Stream[F, A]` of changes (to the part of the model seen by the `Lens`).

Note that `View[F, A]` implements `FixedLens[F, A]`, so the `View` can be used directly whenever a `FixedLens` is needed.

A `View[F, A]` can be further focused on the model by calling `view.zoom(<monocle.Lens[A, B]>)`. This will create a `View[F, B]`.

Also, the `View[F, A]` value can be arbitrarily transformed by a function `f: A => B`, but in this case we lose the write capability of the `Lens`. This can be achieved by calling `view.map(f: A => B)`. This will create a `ViewRO[M, B]`, which provides the usual `fs2.Stream[F, B]` but only the effectful `get` operation from a regular `View/FixedLens`. 

Finally, a `View[F, A]` provides a convenience method `view.algebra[H[_[_]]]` which can be use to invoke an implicit algebra `H` if there's one in scope which acts on the same context `F[_]` as the `View`.

## React integrations and utilities

`import crystal.react._`

Integrates with `scalajs-react`.

`import crystal.react.io.implicits._` provides implicits conversions from `IO` and `SyncIO` to `Callback`.

### Flow

A `Flow[F[_], A]` is a (`scalajs-react`) React component that wraps a `fs.Stream[F, A]`.

It provides a method `.flow(Option[A] => VdomElement)` that can be used within the components to render the received values. It will update whatever is rendered withing block whenever a new value is emitted to the stream.

Note that this is completely indepedant of the core functinality of `crystal`. It can be used with ***any*** `Stream[F, A]`. 

To pass values from the model to components, it should be done as a `View[F, A]` in its properties. For convenience, a `View[F, A]` does provide a `.flow` method that will return a `Flow[F, A]` on its stream.

Also please note that the chosen effect context `F[_]` must have a `cats.effects.ConcurrentEffect` type class instance.