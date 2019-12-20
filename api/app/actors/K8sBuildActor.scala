package io.flow.delta.actors

import akka.actor.{Actor, ActorSystem}
import db._
import io.flow.akka.SafeReceive
import io.flow.delta.lib.StateFormatter
import io.flow.delta.v0.models.{Build, StateForm}
import io.flow.log.RollbarLogger
import k8s.KubernetesService
import scala.concurrent.duration._

object K8sBuildActor {

  trait Factory {
    def apply(buildId: String): Actor
  }

}

class K8sBuildActor @javax.inject.Inject() (
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  kubernetesService: KubernetesService,
  buildLastStatesDao: InternalBuildLastStatesDao,
  usersDao: UsersDao,
  system: ActorSystem,
  override val logger: RollbarLogger,
  @com.google.inject.assistedinject.Assisted buildId: String
) extends Actor with DataBuild {

  private[this] implicit val ec = system.dispatchers.lookup("build-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("K8sBuildActor")

  def receive = SafeReceive.withLogUnhandled {

    case BuildActor.Messages.Setup =>
      handleReceiveSetupEvent()

    case BuildActor.Messages.CheckLastState =>
      withEnabledBuild { build =>
        captureLastState(build)
      }
  }

  private[this] def handleReceiveSetupEvent(): Unit = {
    setBuildId(buildId)

    system.scheduler.schedule(
      Duration(1L, "second"),
      Duration(EcsBuildActor.CheckLastStateIntervalSeconds, "seconds")
    ) {
      self ! BuildActor.Messages.CheckLastState
    }
    ()
  }

  private[this] def captureLastState(build: Build): String = {
    val versions = kubernetesService.getDeployedVersions(build.name)
    buildLastStatesDao.upsert(
      usersDao.systemUser,
      build,
      StateForm(versions = versions)
    )
    StateFormatter.label(versions)
  }

}
