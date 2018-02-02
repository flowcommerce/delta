package io.flow.delta.actors

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

class SupervisorResultSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "merge requires at least 1 result" in {
    intercept[Exception] {
      SupervisorResult.merge(Nil)
    }.getMessage must be("Must have at least 1 result")
  }

  "merge base case" in {
    SupervisorResult.merge(Seq(SupervisorResult.Ready("a"))) must be(SupervisorResult.Ready("a"))
    SupervisorResult.merge(Seq(SupervisorResult.Change("a"))) must be(SupervisorResult.Change("a"))
    SupervisorResult.merge(Seq(SupervisorResult.Checkpoint("a"))) must be(SupervisorResult.Checkpoint("a"))
    SupervisorResult.merge(Seq(SupervisorResult.Error("a"))) must be(SupervisorResult.Error("a"))
  }

  "merge multiple of same time" in {
    SupervisorResult.merge(
      Seq(SupervisorResult.Ready("a"), SupervisorResult.Ready("b"))
    ) must be(SupervisorResult.Ready("a, b"))

    SupervisorResult.merge(
      Seq(SupervisorResult.Change("a"), SupervisorResult.Change("b"))
    ) must be(SupervisorResult.Change("a, b"))

    SupervisorResult.merge(
      Seq(SupervisorResult.Checkpoint("a"), SupervisorResult.Checkpoint("b"))
    ) must be(SupervisorResult.Checkpoint("a, b"))

    SupervisorResult.merge(
      Seq(SupervisorResult.Error("a", None), SupervisorResult.Error("b", None))
    ) must be(SupervisorResult.Error("a, b", None))

    val ex = new Exception()
    SupervisorResult.merge(
      Seq(SupervisorResult.Error("a", Some(ex)), SupervisorResult.Error("b", Some(new Exception())))
    ) must be(SupervisorResult.Error("a, b", Some(ex)))
  }

}
