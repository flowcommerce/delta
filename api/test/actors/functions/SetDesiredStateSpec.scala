package io.flow.delta.actors.functions

import io.flow.delta.actors.SupervisorResult
import io.flow.delta.lib.config.Defaults
import io.flow.test.utils.FlowPlaySpec

class SetDesiredStateSpec extends FlowPlaySpec with db.Helpers {

  "no-op if no tags" in {
    val build = upsertBuild()
    SetDesiredState.run(build, Defaults.Build).map(_ must be(SupervisorResult.Checkpoint("Project does not have any tags")))
  }

  "sets desired state to latest tag" in {
    val project = createProject()
    val build = upsertBuild(project)

    // tag1
    createTag(createTagForm(project).copy(name = "0.0.1"))
    SetDesiredState.run(build, Defaults.Build).map(_ must be(SupervisorResult.Change("Desired state changed to: 0.0.1: 2 instances")))

    // tag2
    createTag(createTagForm(project).copy(name = "0.0.2"))
    SetDesiredState.run(build, Defaults.Build).map(_ must be(SupervisorResult.Change("Desired state changed to: 0.0.2: 2 instances")))

    // No-op if no change
    SetDesiredState.run(build, Defaults.Build).map(_ must be(SupervisorResult.Ready("Desired versions remain: 0.0.2")))
  }

  "once set, desired state does not reset number of instances" in {
    // let ECS manage number of instances on a go forward basis.
    val project = createProject()
    val build = upsertBuild(project)

    // tag1
    createTag(createTagForm(project).copy(name = "0.0.1"))
    SetDesiredState.run(build, Defaults.Build).map(_ must be(SupervisorResult.Change("Desired state changed to: 0.0.1: 2 instances")))

    setLastState(build, "0.0.1", 10)

    // No-op if no change
    SetDesiredState.run(build, Defaults.Build).map(_ must be(SupervisorResult.Ready("Desired versions remain: 0.0.1")))
  }

}
