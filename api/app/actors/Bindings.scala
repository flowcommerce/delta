package io.flow.delta.actors

import actors.RollbarActor
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class ActorsModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActorFactory[BuildActor, BuildActor.Factory]
    bindActorFactory[DockerHubActor, DockerHubActor.Factory]
    bindActorFactory[ProjectActor, ProjectActor.Factory]
    bindActorFactory[DockerHubTokenActor, DockerHubTokenActor.Factory]
    bindActor[MainActor]("main-actor")
    bindActor[RollbarActor]("rollbar-actor")
    bindActorFactory[UserActor, UserActor.Factory]
    bindActorFactory[ProjectSupervisorActor, ProjectSupervisorActor.Factory]
    bindActorFactory[BuildSupervisorActor, BuildSupervisorActor.Factory]
  }
}
