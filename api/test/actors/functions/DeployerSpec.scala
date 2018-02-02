package io.flow.delta.actors.functions

import db.{BuildDesiredStatesDao, BuildLastStatesDao}
import io.flow.delta.v0.models.{Build, State, Version}
import io.flow.delta.actors.SupervisorResult
import io.flow.postgresql.Authorization
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class DeployerSpec extends PlaySpec with OneAppPerSuite with db.Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def last(build: Build): State = {
    BuildLastStatesDao.findByBuildId(Authorization.All, build.id).getOrElse {
      sys.error("No last state")
    }
  }

  def desired(build: Build): State = {
    BuildDesiredStatesDao.findByBuildId(Authorization.All, build.id).getOrElse {
      sys.error("No desired state")
    }
  }

  "scales down versions not listed in last nor desired states before scaling next version" in {
    val project = createProject()
    val build = upsertBuild(project)

    val tag1 = createTag(createTagForm(project).copy(name = "0.0.1"))
    setLastStates(build, Nil)
    SetDesiredState(build).run() must be(
      SupervisorResult.Change("Desired state changed to: 0.0.1: 2 instances")
    )
    Deployer(build, last(build), desired(build)).scale() must be(
      SupervisorResult.Change(s"Scale Up: 0.0.1: Add 2 instances")
    )
    
    val tag2 = createTag(createTagForm(project).copy(name = "0.0.2"))
    SetDesiredState(build).run() must be(
      SupervisorResult.Change("Desired state changed to: 0.0.2: 2 instances")
    )
    Deployer(build, last(build), desired(build)).scale() must be(
      SupervisorResult.Change(s"Scale Up: 0.0.2: Add 2 instances")
    )
    setLastStates(build, Seq(Version("0.0.1", 2), Version("0.0.2", 2)))

    val tag3 = createTag(createTagForm(project).copy(name = "0.0.3"))
    SetDesiredState(build).run() must be(
      SupervisorResult.Change("Desired state changed to: 0.0.3: 2 instances")
    )

    Deployer(build, last(build), desired(build)).scale() must be(
      SupervisorResult.Change(s"Scale Down Extras: 0.0.2: Remove 2 instances")
    )
    setLastStates(build, Seq(Version("0.0.1", 2)))

    Deployer(build, last(build), desired(build)).scale() must be(
      SupervisorResult.Change(s"Scale Up: 0.0.3: Add 2 instances")
    )
  }

}
