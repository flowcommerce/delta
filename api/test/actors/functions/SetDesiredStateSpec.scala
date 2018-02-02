package io.flow.delta.actors.functions

import io.flow.delta.actors.SupervisorResult
import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

class SetDesiredStateSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "no-op if no tags" in {
    val build = upsertBuild()
    SetDesiredState(build).run() must be(SupervisorResult.Checkpoint("Project does not have any tags"))
  }

  "sets desired state to latest tag" in {
    val project = createProject()
    val build = upsertBuild(project)

    val tag1 = createTag(createTagForm(project).copy(name = "0.0.1"))
    SetDesiredState(build).run() must be(SupervisorResult.Change("Desired state changed to: 0.0.1: 2 instances"))

    val tag2 = createTag(createTagForm(project).copy(name = "0.0.2"))
    SetDesiredState(build).run() must be(SupervisorResult.Change("Desired state changed to: 0.0.2: 2 instances"))

    // No-op if no change
    SetDesiredState(build).run() must be(SupervisorResult.Ready("Desired versions remain: 0.0.2"))
  }

  "once set, desired state does not reset number of instances" in {
    // let ECS manage number of instances on a go forward basis.
    val project = createProject()
    val build = upsertBuild(project)

    val tag1 = createTag(createTagForm(project).copy(name = "0.0.1"))
    SetDesiredState(build).run() must be(SupervisorResult.Change("Desired state changed to: 0.0.1: 2 instances"))

    setLastState(build, "0.0.1", 10)

    // No-op if no change
    SetDesiredState(build).run() must be(SupervisorResult.Ready("Desired versions remain: 0.0.1"))
  }

}
