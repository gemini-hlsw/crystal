import scala.concurrent.duration._
import cats.effect._
import fs2.concurrent.SignallingRef

import scala.language.postfixOps
import scala.language.higherKinds
import scala.concurrent.ExecutionContext.Implicits.global

//implicit def timer[F[_] : Timer] = Timer[F]
//implicit def cs[F[_]]: ContextShift[F] = ContextShift[F]

implicit val ioTimer = cats.effect.IO.timer(global)
implicit val ioCS: ContextShift[IO] = IO.contextShift(global)

/*def done[F[_] : Concurrent] =
        SignallingRef.in[SyncIO, F, Boolean](false).unsafeRunSync()*/

def stream[F[_]: Sync : Timer, Int] =
  fs2.Stream.iterateEval(0)(v => Sync[F].delay(v + 1)).metered(1 second)

/*def eval[F[_]: Effect : Timer] =
//  Effect[F].toIO(
//  stream.evalMap(v => Sync[F].delay(println(v))).compile.drain
//  ).start.unsafeRunSync()
  Effect[F].runAsync(
    stream
      .interruptWhen(done)
      .evalMap(v => Sync[F].delay(println(v)))
      .compile.drain
  ){
    _ => IO.unit

//    case Right(value) => IO(value)
//    case Left(error)  => IO.raiseError(error)
  }*/


def evalCancellable[F[_]: ConcurrentEffect : Timer] =
  ConcurrentEffect[F].runCancelable(
    stream
      .evalMap(v => Sync[F].delay(println(v)))
      .compile.drain
  )(_ => IO.unit)

val cancelToken = evalCancellable[IO].unsafeRunSync()


cancelToken.unsafeRunAsyncAndForget()


//def xxx[F[_] : Concurrent : Timer] =
//  stream.evalMap(v => Sync[F].delay(println(v))).compile.drain.start