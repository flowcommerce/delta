package controllers

import io.flow.delta.v0.{Authorization, Client}
import io.flow.delta.v0.errors.{ErrorsResponse, UnitResponse}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import java.util.concurrent.TimeUnit
import play.api.test.WithApplication

trait MockClient extends db.Helpers {

  val DefaultDuration = Duration(5, TimeUnit.SECONDS)

  val port = 9010

  lazy val client = new Client(
    s"http://localhost:$port",
    auth = Some(Authorization.Basic(systemUser.id.toString))
  )

  lazy val anonClient = new Client(s"http://localhost:$port")

  def expectErrors[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ): ErrorsResponse = {
    Try(
      Await.result(f, duration)
    ) match {
      case Success(response) => {
        sys.error("Desired function to fail but it succeeded with: " + response)
      }
      case Failure(ex) =>  ex match {
        case e: ErrorsResponse => {
          e
        }
        case e => {
          sys.error(s"Desired an exception of type[ErrorsResponse] but got[$e]")
        }
      }
    }
  }

  def expectNotFound[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ) {
    expectStatus(404) {
      Await.result(f, duration)
    }
  }

  def expectNotAuthorized[T](
    f: => Future[T],
    duration: Duration = DefaultDuration
  ) {
    expectStatus(401) {
      Await.result(f, duration)
    }
  }

  def expectStatus(code: Int)(f: => Unit) {
    Try(
      f
    ) match {
      case Success(response) => {
        org.specs2.execute.Failure(s"Desired HTTP[$code] but got HTTP 2xx")
      }
      case Failure(ex) => ex match {
        case UnitResponse(code) => {
          org.specs2.execute.Success()
        }
        case e => {
          org.specs2.execute.Failure(s"Undesired error: $e")
        }
      }
    }
  }
}

