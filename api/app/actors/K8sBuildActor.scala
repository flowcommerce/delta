package io.flow.delta.actors

import akka.actor.{Actor, ActorSystem}
import db._
import io.flow.akka.SafeReceive
import io.flow.delta.api.lib.EventLogProcessor
import io.flow.delta.v0.models.{Build, StateForm}
import io.flow.log.RollbarLogger
import k8s.KubernetesService
import k8s.KubernetesService.toDeploymentName

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

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
  eventLogProcessor: EventLogProcessor,
  override val logger: RollbarLogger,
  @com.google.inject.assistedinject.Assisted buildId: String
) extends Actor with DataBuild {

  private[this] implicit val ec = system.dispatchers.lookup("build-actor-context")
  private[this] implicit val configuredRollbar = logger
    .fingerprint("K8sBuildActor")
    .withKeyValue("build_id", buildId)
  configuredRollbar.info(s"K8sBuildActor created for build[$buildId]")

  def receive = SafeReceive.withLogUnhandled {

    case BuildActor.Messages.Setup =>
      configuredRollbar.info(s"K8sBuildActor BuildActor.Messages.Setup for build[${buildId}]")
      handleReceiveSetupEvent()

    case BuildActor.Messages.CheckLastState =>
      configuredRollbar.info(s"K8sBuildActor BuildActor.Messages.CheckLastState for build[${buildId}]")
      withEnabledBuild { build =>
        eventLogProcessor.runSync("CheckLastState", log = log(build.project.id)) {
          configuredRollbar.info(s"K8sBuildActor BuildActor.Messages.CheckLastState for build[${buildId}] with enabled build")
          captureLastState(build)
        }
      }
  }

  private[this] def handleReceiveSetupEvent(): Unit = {
    setBuildId(buildId)

    system.scheduler.scheduleWithFixedDelay(
      Duration(1L, "second"),
      Duration(EcsBuildActor.CheckLastStateIntervalSeconds, "seconds")
    ) {
      () => self ! BuildActor.Messages.CheckLastState
    }
    ()
  }

  private[this] def captureLastState(build: Build): Unit = {
    Try {
      val deploymentName = toDeploymentName(build)
      configuredRollbar
        .withKeyValue("build_name", build.name)
        .withKeyValue("build_project_id", build.project.id)
        .withKeyValue("deploymentName", deploymentName)
        .info("CaptureLastState")
      kubernetesService.getDeployedVersions(deploymentName)
    } match {
      case Success(versions) => {
        buildLastStatesDao.upsert(
          usersDao.systemUser,
          build,
          StateForm(versions = versions)
        )
        ()
      }
      case Failure(ex) => {
        log(build).warn("Error getting deployed versions from k8s", ex)
      }
    }
  }

  private[this] def log(build: Build): RollbarLogger = {
    logger
      .organization(build.project.organization.id)
      .withKeyValue("project_id", build.project.id)
      .withKeyValue("build_name", build.name)
  }

}
