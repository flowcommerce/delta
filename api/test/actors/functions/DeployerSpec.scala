package io.flow.delta.actors.functions

import akka.actor.ActorRef
import io.flow.delta.actors.SupervisorResult
import io.flow.delta.v0.models.{Build, State, Version}
import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec
import play.api.inject.BindingKey

class DeployerSpec extends FlowPlaySpec with db.Helpers {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val mainActor =  app.injector.instanceOf(BindingKey(classOf[ActorRef]).qualifiedWith("main-actor"))

  def last(build: Build): State = {
    buildLastStatesDao.findByBuildId(Authorization.All, build.id).getOrElse {
      sys.error("No last state")
    }
  }

  def desired(build: Build): State = {
    buildDesiredStatesDao.findByBuildId(Authorization.All, build.id).getOrElse {
      sys.error("No desired state")
    }
  }

  "scales down versions not listed in last nor desired states before scaling next version" in {
    val project = createProject()
    val build = upsertBuild(project)

    val tag1 = createTag(createTagForm(project).copy(name = "0.0.1"))
    setLastStates(build, Nil)
    await(SetDesiredState.run(build)) must be(
      SupervisorResult.Change("Desired state changed to: 0.0.1: 2 instances")
    )
    Deployer(build, last(build), desired(build), mainActor).scale() must be(
      SupervisorResult.Change(s"Scale Up: 0.0.1: Add 2 instances")
    )
    
    val tag2 = createTag(createTagForm(project).copy(name = "0.0.2"))
    await(SetDesiredState.run(build)) must be(
      SupervisorResult.Change("Desired state changed to: 0.0.2: 2 instances")
    )
    Deployer(build, last(build), desired(build), mainActor).scale() must be(
      SupervisorResult.Change(s"Scale Up: 0.0.2: Add 2 instances")
    )
    setLastStates(build, Seq(Version("0.0.1", 2), Version("0.0.2", 2)))

    val tag3 = createTag(createTagForm(project).copy(name = "0.0.3"))
    await(SetDesiredState.run(build)) must be(
      SupervisorResult.Change("Desired state changed to: 0.0.3: 2 instances")
    )

    Deployer(build, last(build), desired(build), mainActor).scale() must be(
      SupervisorResult.Change(s"Scale Down Extras: 0.0.2: Remove 2 instances")
    )
    setLastStates(build, Seq(Version("0.0.1", 2)))

    Deployer(build, last(build), desired(build), mainActor).scale() must be(
      SupervisorResult.Change(s"Scale Up: 0.0.3: Add 2 instances")
    )
  }

}
