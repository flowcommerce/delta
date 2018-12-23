package io.flow.delta.actors

import akka.actor.{Actor, ActorSystem}
import db._
import io.flow.common.v0.models.UserReference
import io.flow.delta.api.lib.{GitHubHelper, Github, Repo}
import io.flow.delta.lib.config.Parser
import io.flow.delta.v0.models.Project
import io.flow.github.v0.models.{HookConfig, HookEvent, HookForm}
import io.flow.log.RollbarLogger
import io.flow.play.actors.ErrorHandler
import io.flow.play.util.{Config, Constants}
import io.flow.postgresql.Authorization

import scala.concurrent.duration._

object ProjectActor {

  val SyncIfInactiveIntervalMinutes = 15

  trait Message

  object Messages {
    case object Setup extends Message
    case object SyncBuilds extends Message
    case object SyncConfig extends Message
    case object SyncIfInactive extends Message
  }

  trait Factory {
    def apply(projectId: String): Actor
  }

}

class ProjectActor @javax.inject.Inject() (
  config: Config,
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  system: ActorSystem,
  github: Github,
  parser: Parser,
  gitHubHelper: GitHubHelper,
  eventsDao: EventsDao,
  override val logger: RollbarLogger,
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  @com.google.inject.assistedinject.Assisted projectId: String
) extends Actor with ErrorHandler with DataBuild with DataProject with EventLog {

  private[this] implicit val ec = system.dispatchers.lookup("project-actor-context")

  private[this] def log(project: Project): RollbarLogger = {
    logger.
      withKeyValue("user_id", project.user.id).
      withKeyValue("project_id", project.id).
      withKeyValue("project_name", project.name)
  }

  def receive = {

    case msg @ ProjectActor.Messages.Setup => withErrorHandler(msg) {
      setProjectId(projectId)

      withProject { project =>
        withRepo { repo =>
          createHooks(project, repo)
        }
      }

      system.scheduler.schedule(
        Duration(ProjectActor.SyncIfInactiveIntervalMinutes, "minutes"),
        Duration(ProjectActor.SyncIfInactiveIntervalMinutes, "minutes")
      ) {
        self ! ProjectActor.Messages.SyncIfInactive
      }
    }

    case msg @ ProjectActor.Messages.SyncBuilds => withErrorHandler(msg) {
      buildsDao.findAllByProjectId(Authorization.All, projectId).foreach { build =>
        mainActor ! MainActor.Messages.BuildDesiredStateUpdated(build.id)
      }
    }

    case msg @ ProjectActor.Messages.SyncConfig => withErrorHandler(msg) {
      withProject { project =>
        withRepo { repo =>
          github.dotDeltaFile(UserReference(project.user.id), repo.owner, repo.project).map {
            case None => {
              log(project).warn("Project repo is missing a .delta file - cannot configure")
            }
            case Some(cfg) => {
              val latestConfig = parser.parse(cfg)
              configsDao.updateIfChanged(Constants.SystemUser, project.id, latestConfig)
            }
          }
        }
      }
    }

    case msg @ ProjectActor.Messages.SyncIfInactive => withErrorHandler(msg) {
      withProject { project =>
        eventsDao.findAll(
          projectId = Some(project.id),
          numberMinutesSinceCreation = Some(5),
          limit = 1
        ).headOption match {
          case Some(_) => // No-op as there is recent activity in the event log
          case None => mainActor ! MainActor.Messages.ProjectSync(project.id)
        }
      }
    }

    case msg: Any => logUnhandledMessage(msg)

  }

  private[this] val HookBaseUrl = config.requiredString("delta.api.host") + "/webhooks/github/"
  private[this] val HookName = "web"
  private[this] val HookEvents = Seq(HookEvent.Push)

  private[this] def createHooks(project: Project, repo: Repo) {
    gitHubHelper.apiClientFromUser(project.user.id) match {
      case None => {
        log(project).warn("Could not create github client")
      }
      case Some(client) => {
        client.hooks.get(repo.owner, repo.project).map { hooks =>
          val targetUrl = HookBaseUrl + project.id

          hooks.find(_.config.url == Some(targetUrl)) match {
            case Some(hook) => {
              // No-op hook exists
            }
            case None => {
              client.hooks.post(
                owner = repo.owner,
                repo = repo.project,
                HookForm(
                  name = HookName,
                  config = HookConfig(
                    url = Some(targetUrl),
                    contentType = Some("json")
                  ),
                  events = HookEvents,
                  active = true
                )
              )
            }.map { hook =>
              log(project).withKeyValue("hook", hook.name).info("Created githib webhook for project")
            }.recover {
              case e: Throwable => {
                log(project).error("Error creating hook", e)
              }
            }
          }
        }
      }
    }
  }
}
