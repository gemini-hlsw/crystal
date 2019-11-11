package com.rpiaggio.crystal.effects

import cats.effect.Async
import japgolly.scalajs.react.AsyncCallback
import AsyncCallbackEffects._

import scala.util.Left

object ATest {

  println("fewfewfewf*/*/*/*/")

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future
  import scala.util.{Success, Failure}

  val apiCall = Future.successful("I come from the Future!")

  val ioa: AsyncCallback[String] =
    Async[AsyncCallback].async { cb =>
      import scala.util.{Failure, Success}

      apiCall.onComplete {
        case Success(value) => println("COMPLETE"); cb(Right(value))
        case Failure(error) => cb(Left(error))
      }
    }

  ioa.unsafeToFuture.map(s => println(s"********************************2222 $s"))
  apiCall.map(s => println(s"********************************1111 $s"))
}
