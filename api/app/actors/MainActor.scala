package io.flow.delta.actors

import java.util.UUID

import actors.RollbarActor
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import com.typesafe.config.Config
import db.{BuildsDao, ItemsDao, ProjectsDao}
import io.flow.akka.SafeReceive
import io.flow.akka.recurring.{ScheduleConfig, Scheduler}
import io.flow.common.v0.models.UserReference
import io.flow.delta.api.lib.StateDiff
import io.flow.log.RollbarLogger
import io.flow.util.Constants
import io.flow.postgresql.{Authorization, Pager}
import javax.inject.Named
import play.api.libs.concurrent.InjectedActorSupport
import play.api.{Environment, Mode}

object MainActor {

  lazy val SystemUser: UserReference = Constants.SystemUser

  object Messages {

    case class BuildDockerImage(buildId: String, version: String)
    case class CheckLastState(buildId: String)

    case class ProjectCreated(id: String)
    case class ProjectUpdated(id: String)
    case class ProjectDeleted(id: String)
    case class ProjectSync(id: String)

    case class BuildCreated(id: String)
    case class BuildUpdated(id: String)
    case class BuildDeleted(id: String)
    case class BuildSync(id: String)
    case class BuildCheckTag(id: String, name: String)

    case class BuildDesiredStateUpdated(buildId: String)
    case class BuildLastStateUpdated(buildId: String)

    case class Scale(buildId: String, diffs: Seq[StateDiff])

    case class ShaUpserted(projectId: String, id: String)

    case class TagCreated(projectId: String, id: String, name: String)
    case class TagUpdated(projectId: String, id: String, name: String)

    case class UserCreated(id: String)

    case class ImageCreated(buildId: String, id: String, version: String)

    case class ConfigureAWS(buildId: String)

    case class EnsureContainerAgentHealth(buildId: String)
    case class UpdateContainerAgent(buildId: String)
    case class RemoveOldServices(buildId: String)
  }
}

@javax.inject.Singleton
class MainActor @javax.inject.Inject() (
  logger: RollbarLogger,
  config: Config,
  buildFactory: BuildActor.Factory,
  dockerHubFactory: DockerHubActor.Factory,
  dockerHubTokenFactory: DockerHubTokenActor.Factory,
  projectFactory: ProjectActor.Factory,
  userActorFactory: UserActor.Factory,
  projectSupervisorActorFactory: ProjectSupervisorActor.Factory,
  buildSupervisorActorFactory: BuildSupervisorActor.Factory,
  system: ActorSystem,
  playEnv: Environment,
  buildsDao: BuildsDao,
  projectsDao: ProjectsDao,
  itemsDao: ItemsDao,
  @Named("rollbar-actor") rollbarActor: ActorRef,
) extends Actor with ActorLogging with Scheduler with InjectedActorSupport {

  private[this] implicit val ec = system.dispatchers.lookup("main-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("MainActor")

  private[this] val name = "main"

  private[this] val searchActor = system.actorOf(Props[SearchActor](new SearchActor(logger, projectsDao, itemsDao)), name = s"$name:SearchActor")
  private[this] val dockerHubTokenActor = injectedChild(dockerHubTokenFactory(), name = s"main:DockerHubActor")

  private[this] val buildActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val buildSupervisorActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val dockerHubActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val projectActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val projectSupervisorActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val userActors = scala.collection.mutable.Map[String, ActorRef]()

  playEnv.mode match {
    case Mode.Test => {
      logger.info("[MainActor] Background actors are disabled in Test")
    }

    case _ => {
      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.update.jwt.token"),
        DockerHubTokenActor.Messages.Refresh,
        dockerHubTokenActor
      )

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.ensure.container.agent.health")
      ){
        Pager.create { offset =>
          buildsDao.findAll(Authorization.All, offset = offset)
        }.foreach { build =>
          self ! MainActor.Messages.EnsureContainerAgentHealth(build.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.update.container.agent")
      ) {
        Pager.create { offset =>
          buildsDao.findAll(Authorization.All, offset = offset)
        }.foreach { build =>
          self ! MainActor.Messages.UpdateContainerAgent(build.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.remove.old.services")
      ) {
        Pager.create { offset =>
          buildsDao.findAll(Authorization.All, offset = offset)
        }.foreach { build =>
          self ! MainActor.Messages.RemoveOldServices(build.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.project.sync")
      ) {
        Pager.create { offset =>
          projectsDao.findAll(Authorization.All, offset = offset)
        }.foreach { project =>
          self ! MainActor.Messages.ProjectSync(project.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.project.inactive.check")
      ) {
        Pager.create { offset =>
          projectsDao.findAll(Authorization.All, offset = offset, minutesSinceLastEvent = Some(15))
        }.foreach { project =>
          logger.withKeyValue("project_id", project.id).info("Sending ProjectSync - no events found in last 15 minutes")
          self ! MainActor.Messages.ProjectSync(project.id)
        }
      }
    }
  }

  def receive = playEnv.mode match {
    case Mode.Test => {
      case msg => {
        logger.withKeyValue("message", msg.toString).info("[MainActor TEST] Discarding received message")
      }
    }

    case _ => SafeReceive.withLogUnhandled {
      case MainActor.Messages.BuildCreated(id) =>
        upsertBuildSupervisorActor(id) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.BuildUpdated(id) =>
        upsertBuildSupervisorActor(id) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.BuildDeleted(id) =>
        (buildActors -= id).foreach { case (_, actor) =>
          actor ! BuildActor.Messages.Delete
          actor ! PoisonPill
        }

      case MainActor.Messages.BuildSync(id) =>
        upsertBuildSupervisorActor(id) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.BuildCheckTag(id, name) =>
        upsertBuildSupervisorActor(id) ! BuildSupervisorActor.Messages.CheckTag(name)

      case MainActor.Messages.UserCreated(id) =>
        upsertUserActor(id) ! UserActor.Messages.Created

      case MainActor.Messages.ProjectCreated(id) =>
        self ! MainActor.Messages.ProjectSync(id)

      case MainActor.Messages.ProjectUpdated(id) =>
        searchActor ! SearchActor.Messages.SyncProject(id)
        upsertProjectSupervisorActor(id) ! ProjectSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.ProjectDeleted(id) =>
        searchActor ! SearchActor.Messages.SyncProject(id)

        (projectActors -= id).foreach { case (_, actor) =>
          actor ! PoisonPill
        }

        (projectSupervisorActors -= id).foreach { case (_, actor) =>
          actor ! PoisonPill
        }

      case MainActor.Messages.ProjectSync(id) =>
        val ref = upsertProjectActor(id)
        ref ! ProjectActor.Messages.SyncConfig
        ref ! ProjectActor.Messages.SyncBuilds
        upsertProjectSupervisorActor(id) ! ProjectSupervisorActor.Messages.PursueDesiredState
        searchActor ! SearchActor.Messages.SyncProject(id)

      case MainActor.Messages.Scale(buildId, diffs) =>
        rollbarActor ! RollbarActor.Messages.Deployment(buildId, diffs)
        upsertBuildActor(buildId) ! BuildActor.Messages.Scale(diffs)

      case MainActor.Messages.ShaUpserted(projectId, _) =>
        upsertProjectSupervisorActor(projectId) ! ProjectSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.TagCreated(projectId, _, name) =>
        upsertProjectSupervisorActor(projectId) ! ProjectSupervisorActor.Messages.CheckTag(name)

      case MainActor.Messages.TagUpdated(projectId, _, name) =>
        upsertProjectSupervisorActor(projectId) ! ProjectSupervisorActor.Messages.CheckTag(name)

      case MainActor.Messages.ImageCreated(buildId, _, version) =>
        upsertBuildSupervisorActor(buildId) ! BuildSupervisorActor.Messages.CheckTag(version)

      case MainActor.Messages.BuildDockerImage(buildId, version) =>
        upsertDockerHubActor(buildId) ! DockerHubActor.Messages.Build(version)

      case MainActor.Messages.CheckLastState(buildId) =>
        upsertBuildActor(buildId) ! BuildActor.Messages.CheckLastState

      case MainActor.Messages.BuildDesiredStateUpdated(buildId) =>
        upsertBuildSupervisorActor(buildId) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.BuildLastStateUpdated(buildId) =>
        upsertBuildSupervisorActor(buildId) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.ConfigureAWS(buildId) =>
        upsertBuildActor(buildId) ! BuildActor.Messages.ConfigureAWS

      case MainActor.Messages.RemoveOldServices(buildId) =>
        upsertBuildActor(buildId) ! BuildActor.Messages.RemoveOldServices

      case MainActor.Messages.UpdateContainerAgent(buildId) =>
        upsertBuildActor(buildId) ! BuildActor.Messages.UpdateContainerAgent

      case MainActor.Messages.EnsureContainerAgentHealth(buildId) =>
        upsertBuildActor(buildId) ! BuildActor.Messages.EnsureContainerAgentHealth
    }
  }

  def upsertDockerHubActor(buildId: String): ActorRef = {
    this.synchronized {
      dockerHubActors.lift(buildId).getOrElse {
        val ref = injectedChild(dockerHubFactory(buildId), name = randomName())
        ref ! DockerHubActor.Messages.Setup
        dockerHubActors += (buildId -> ref)
        ref
      }
    }
  }

  def upsertUserActor(id: String): ActorRef = {
    this.synchronized {
      userActors.lift(id).getOrElse {
        val ref = injectedChild(userActorFactory(id), name = randomName())
        ref ! UserActor.Messages.Data(id)
        userActors += (id -> ref)
        ref
     }
    }
  }

  def upsertProjectActor(id: String): ActorRef = {
    this.synchronized {
      projectActors.lift(id).getOrElse {
        val ref = injectedChild(projectFactory(id), name = randomName())
        ref ! ProjectActor.Messages.Setup
        projectActors += (id -> ref)
        ref
      }
    }
  }

  def upsertBuildActor(id: String): ActorRef = {
    this.synchronized {
      buildActors.lift(id).getOrElse {
        val ref = injectedChild(buildFactory(id), name = randomName())
        ref ! BuildActor.Messages.Setup
        buildActors += (id -> ref)
        ref
      }
    }
  }

  def upsertProjectSupervisorActor(id: String): ActorRef = {
    this.synchronized {
      projectSupervisorActors.lift(id).getOrElse {
        val ref = injectedChild(projectSupervisorActorFactory(id), name = randomName())
        ref ! ProjectSupervisorActor.Messages.Data(id)
        projectSupervisorActors += (id -> ref)
        ref
      }
    }
  }

  def upsertBuildSupervisorActor(id: String): ActorRef = {
    this.synchronized {
      buildSupervisorActors.getOrElse(id, {
        val ref = injectedChild(buildSupervisorActorFactory(id), name = randomName())
        ref ! BuildSupervisorActor.Messages.Data(id)
        buildSupervisorActors += (id -> ref)
        ref
      })
    }
  }

  private[this] def randomName(): String = {
    s"$name:" + UUID.randomUUID().toString
  }
}
