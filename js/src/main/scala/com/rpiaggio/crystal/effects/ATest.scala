package com.rpiaggio.crystal.effects

import cats.effect.Async
import japgolly.scalajs.react.AsyncCallback
import AsyncCallbackEffects._
import cats.effect.IO.Delay

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.util.{Either, Left}

/*object ATest {

  println("fewfewfewf***")


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

object BTest {

  println("fewfewfewf*****")

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future
  import scala.util.{Success, Failure}

  val apiCall = Future.successful("I come from the Future!")

  val ioa: AsyncCallback[String] =
    Async[AsyncCallback].asyncF { cb =>
      import scala.util.{Failure, Success}

      AsyncCallback.point(
        apiCall.onComplete {
          case Success(value) => println("COMPLETE"); cb(Right(value))
          case Failure(error) => cb(Left(error))
        }
      )
    }

  ioa.unsafeToFuture.map(s => println(s"********************************2222 $s"))
  apiCall.map(s => println(s"********************************1111 $s"))
}*/

object X {

  val apiCall = Future.successful("I come from the Future!")

  val k: (Either[Throwable, String] => Unit) => Unit = { cb =>
    import scala.util.{Failure, Success}

    apiCall.onComplete {
      case Success(value) => println(s"COMPLETE WITH [$value]"); cb(Right(value))
      case Failure(error) => cb(Left(error))
    }
  }

    def asyncCanBeDerivedFromAsyncF[A](k: (Either[Throwable, A] => Unit) => Unit) = {
      val ls = Async[AsyncCallback].async(k)

      val rs = Async[AsyncCallback].asyncF{cb: (Either[Throwable, A] => Unit) =>
        Async[AsyncCallback].delay(k(cb))}

      println("******* LS *******")
//      println(ls.toCallback.runNow())
      ls.unsafeToFuture().map(println)
      println("******************")

      println("******* RS *******")
//      println(rs.toCallback.runNow())
      rs.unsafeToFuture().map(println)
      println("******************")
    }

  def go = asyncCanBeDerivedFromAsyncF(k)
}
