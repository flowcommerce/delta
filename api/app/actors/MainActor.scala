package io.flow.delta.actors

import java.util.UUID

import actors.RollbarActor
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import com.typesafe.config.Config
import db.{BuildsDao, ConfigsDao, DashboardBuildsDao, ItemsDao, ProjectsDao}
import io.flow.akka.SafeReceive
import io.flow.akka.recurring.{ScheduleConfig, Scheduler}
import io.flow.common.v0.models.UserReference
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.config.v0.models.{Cluster, ConfigError, ConfigProject, ConfigUndefinedType}
import io.flow.delta.v0.models.DashboardBuild
import io.flow.log.RollbarLogger
import io.flow.util.Constants
import io.flow.postgresql.Authorization
import javax.inject.Named
import lib.ProjectConfigUtil
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
  ecsBuildFactory: EcsBuildActor.Factory,
  k8sBuildFactory: K8sBuildActor.Factory,
  dockerHubFactory: DockerHubActor.Factory,
  dockerHubTokenFactory: DockerHubTokenActor.Factory,
  projectFactory: ProjectActor.Factory,
  userActorFactory: UserActor.Factory,
  projectSupervisorActorFactory: ProjectSupervisorActor.Factory,
  buildSupervisorActorFactory: BuildSupervisorActor.Factory,
  system: ActorSystem,
  playEnv: Environment,
  dashboardBuildsDao: DashboardBuildsDao,
  buildsDao: BuildsDao,
  configsDao: ConfigsDao,
  projectsDao: ProjectsDao,
  itemsDao: ItemsDao,
  @Named("rollbar-actor") rollbarActor: ActorRef,
) extends Actor with ActorLogging with Scheduler with InjectedActorSupport {

  private[this] implicit val ec = system.dispatchers.lookup("main-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("MainActor")

  private[this] val name = "main"

  private[this] val searchActor = system.actorOf(Props[SearchActor](new SearchActor(logger, projectsDao, itemsDao)), name = s"$name:SearchActor")
  private[this] val dockerHubTokenActor = injectedChild(dockerHubTokenFactory(), name = s"main:DockerHubActor")

  private[this] val ecsBuildActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val k8sBuildActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val buildSupervisorActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val dockerHubActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val projectActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val projectSupervisorActors = scala.collection.mutable.Map[String, ActorRef]()
  private[this] val userActors = scala.collection.mutable.Map[String, ActorRef]()

  private[this] def allBuilds(cluster: Cluster): Seq[DashboardBuild] = {
    dashboardBuildsDao.findAll(Authorization.All, limit = None).filter(_.cluster == cluster)
  }

  playEnv.mode match {
    case Mode.Test => {
      logger.info("[MainActor] Background actors are disabled in Test")
    }

    case _ => {
      logger.info("MainActor Starting")
      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.update.jwt.token"),
        DockerHubTokenActor.Messages.Refresh,
        dockerHubTokenActor
      )

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.ensure.container.agent.health")
      ){
        allBuilds(Cluster.Ecs).foreach { build =>
          self ! MainActor.Messages.EnsureContainerAgentHealth(build.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.update.container.agent")
      ) {
        allBuilds(Cluster.Ecs).foreach { build =>
          self ! MainActor.Messages.UpdateContainerAgent(build.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.remove.old.services")
      ) {
        allBuilds(Cluster.Ecs).foreach { build =>
          self ! MainActor.Messages.RemoveOldServices(build.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.project.sync")
      ) {
        projectsDao.findAll(Authorization.All, limit = None).foreach { project =>
          self ! MainActor.Messages.ProjectSync(project.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.project.sync")
      ){
        // make sure we have actors for all k8s builds
        allBuilds(Cluster.K8s).foreach { build =>
          self ! MainActor.Messages.CheckLastState(build.id)
        }
      }

      scheduleRecurring(
        ScheduleConfig.fromConfig(config, "main.actor.project.inactive.check")
      ) {
        projectsDao.findAll(Authorization.All, limit = None, minutesSinceLastEvent = Some(15)).foreach { project =>
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
        (ecsBuildActors -= id).foreach { case (_, actor) =>
          actor ! EcsBuildActor.Messages.Delete
          actor ! PoisonPill
        }

      case MainActor.Messages.BuildSync(id) =>
        upsertBuildSupervisorActor(id) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.BuildCheckTag(id, name) =>
        upsertBuildSupervisorActor(id) ! BuildSupervisorActor.Messages.CheckTag(name)

      case MainActor.Messages.UserCreated(id) =>
        upsertUserActor(id) ! UserActor.Messages.Created

      case MainActor.Messages.ProjectCreated(id) =>
        logger.withKeyValue("project_id", id).info("MainActor.Messages.ProjectCreated")
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
        upsertBuildActor(buildId) ! EcsBuildActor.Messages.Scale(diffs)

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
        upsertBuildActor(buildId) ! EcsBuildActor.Messages.CheckLastState

      case MainActor.Messages.BuildDesiredStateUpdated(buildId) =>
        upsertBuildSupervisorActor(buildId) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.BuildLastStateUpdated(buildId) =>
        upsertBuildSupervisorActor(buildId) ! BuildSupervisorActor.Messages.PursueDesiredState

      case MainActor.Messages.ConfigureAWS(buildId) =>
        upsertBuildActor(buildId) ! EcsBuildActor.Messages.ConfigureAWS

      case MainActor.Messages.RemoveOldServices(buildId) =>
        upsertBuildActor(buildId) ! EcsBuildActor.Messages.RemoveOldServices

      case MainActor.Messages.UpdateContainerAgent(buildId) =>
        upsertBuildActor(buildId) ! EcsBuildActor.Messages.UpdateContainerAgent

      case MainActor.Messages.EnsureContainerAgentHealth(buildId) =>
        upsertBuildActor(buildId) ! EcsBuildActor.Messages.EnsureContainerAgentHealth
    }
  }

  def upsertDockerHubActor(buildId: String): ActorRef = {
    this.synchronized {
      dockerHubActors.getOrElse(buildId, {
        val ref = injectedChild(dockerHubFactory(buildId), name = randomName(), _.withDispatcher("io-dispatcher"))
        ref ! DockerHubActor.Messages.Setup
        dockerHubActors += (buildId -> ref)
        ref
      })
    }
  }

  def upsertUserActor(id: String): ActorRef = {
    this.synchronized {
      userActors.getOrElse(id, {
        val ref = injectedChild(userActorFactory(id), name = randomName(), _.withDispatcher("io-dispatcher"))
        ref ! UserActor.Messages.Data(id)
        userActors += (id -> ref)
        ref
      })
    }
  }

  def upsertProjectActor(id: String): ActorRef = {
    this.synchronized {
      projectActors.getOrElse(id, {
        val ref = injectedChild(projectFactory(id), name = randomName(), _.withDispatcher("io-dispatcher"))
        ref ! ProjectActor.Messages.Setup
        projectActors += (id -> ref)
        ref
      })
    }
  }

  def upsertBuildActor(id: String): ActorRef = {
    getClusterByBuildId(id).getOrElse {
      sys.error(s"Build ${id}: Cannot determine cluster")
    } match {
      case Cluster.Ecs => upsertEcsBuildActor(id)
      case Cluster.K8s => upsertK8sBuildActor(id)
      case Cluster.UNDEFINED(other) => sys.error(s"Build ${id}: Invalid cluster '$other'")
    }
  }

  private[this] def upsertEcsBuildActor(id: String): ActorRef = {
    this.synchronized {
      ecsBuildActors.getOrElse(id, {
        val ref = injectedChild(ecsBuildFactory(id), name = randomName(), _.withDispatcher("io-dispatcher"))
        ref ! EcsBuildActor.Messages.Setup
        ecsBuildActors += (id -> ref)
        ref
      })
    }
  }

  private[this] def upsertK8sBuildActor(id: String): ActorRef = {
    this.synchronized {
      k8sBuildActors.getOrElse(id, {
        val ref = injectedChild(k8sBuildFactory(id), name = randomName(), _.withDispatcher("io-dispatcher"))
        ref ! EcsBuildActor.Messages.Setup
        k8sBuildActors += (id -> ref)
        ref
      })
    }
  }

  def upsertProjectSupervisorActor(id: String): ActorRef = {
    this.synchronized {
      projectSupervisorActors.getOrElse(id, {
        val ref = injectedChild(projectSupervisorActorFactory(id), name = randomName(), _.withDispatcher("io-dispatcher"))
        ref ! ProjectSupervisorActor.Messages.Data(id)
        projectSupervisorActors += (id -> ref)
        ref
      })
    }
  }

  def upsertBuildSupervisorActor(id: String): ActorRef = {
    this.synchronized {
      buildSupervisorActors.getOrElse(id, {
        val ref = injectedChild(buildSupervisorActorFactory(id), name = randomName(), _.withDispatcher("io-dispatcher"))
        ref ! BuildSupervisorActor.Messages.Data(id)
        buildSupervisorActors += (id -> ref)
        ref
      })
    }
  }

  private[this] def randomName(): String = {
    s"$name:" + UUID.randomUUID().toString
  }

  private[this] def getClusterByBuildId(buildId: String): Option[Cluster] = {
    buildsDao.findById(Authorization.All, buildId).flatMap { build =>
      configsDao.findByProjectId(Authorization.All, build.project.id).flatMap { config =>
        config.config match {
          case c: ConfigProject => ProjectConfigUtil.cluster(c, build.name)
          case _: ConfigError | _: ConfigUndefinedType => None
        }
      }
    }
  }

}

