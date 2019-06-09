package actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import db.{BuildsDao, TagsDao}
import io.flow.akka.SafeReceive
import io.flow.akka.recurring.{ScheduleConfig, Scheduler}
import io.flow.delta.api.lib.StateDiff
import io.flow.log.RollbarLogger
import io.flow.play.util.ApplicationConfig
import io.flow.postgresql.Authorization
import io.flow.rollbar.v0.models.Deploy
import io.flow.rollbar.v0.{Client => Rollbar}
import io.flow.util.FlowEnvironment
import javax.inject.{Inject, Singleton}
import play.api.{Application, Mode}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object RollbarActor {

  trait Message

  object Messages {
    case class Deployment(buildId: String, diffs: Seq[StateDiff]) extends Message
    case object Refresh extends Message
  }

}

@Singleton
class RollbarActor @Inject()(
  app: Application,
  logger: RollbarLogger,
  ws: WSClient,
  system: ActorSystem,
  buildsDao: BuildsDao,
  tagsDao: TagsDao,
  val config: ApplicationConfig
) extends Actor with ActorLogging with Scheduler {

  private implicit val ec: ExecutionContext = system.dispatchers.lookup("rollbar-actor-context")
  private[this] implicit val configuredRollbar: RollbarLogger = logger.fingerprint("RollbarActor")

  private val rollbar = new Rollbar(ws)

  private val ConfigName = "rollbar.account_token"
  private val accessToken = config.optionalString(ConfigName)
  if (app.mode == Mode.Prod && accessToken.isEmpty) {
    configuredRollbar
      .withKeyValue("config", ConfigName)
      .warn(s"Rollbar will not know about deploys because rollbar.account_token (ROLLBAR_ACCESS_TOKEN in env) is missing")
  }

  private case class Project(name: String, id: Int, postAccessKey: String)

  private val projectCache = new java.util.concurrent.ConcurrentHashMap[String, Project]()

  scheduleRecurring(
    ScheduleConfig.fromConfig(config.underlying.underlying, "rollbar.actor.refresh"),
    RollbarActor.Messages.Refresh
  )

  def receive = SafeReceive.withLogUnhandled {

    case RollbarActor.Messages.Deployment(buildId, diffs) =>
      accessToken.foreach { _ =>
        buildsDao.findById(Authorization.All, buildId) match {
          case None => throw new IllegalArgumentException(buildId)
          case Some(build) =>
            diffs.headOption match { // do we ever have more than 1 statediff?
              case None => throw new IllegalArgumentException(diffs.toString)
              case Some(diff) =>
                // log to determine whether we should only post when lastInstances == 0
                val log = configuredRollbar
                  .withKeyValue("config", ConfigName)
                  .withKeyValue("project", build.project.id)

                log
                  .withKeyValue("project_id", build.project.id)
                  .withKeyValue("build_id", build.id)
                  .withKeyValue("build_name", build.name)
                  .withKeyValue("version", diff.versionName)
                  .withKeyValue("desired_instances", diff.desiredInstances)
                  .withKeyValue("last_instances", diff.lastInstances)
                  .info("got deploy")

                if (diff.desiredInstances > diff.lastInstances) { // scale up
                  tagsDao.findByProjectIdAndName(Authorization.All, build.project.id, diff.versionName) match {
                    case None => {
                      log.warn("Could not find tag for project and version")
                    }
                    case Some(tag) => {
                      if (projectCache.containsKey(build.project.id)) {
                        rollbar.deploys.post(Deploy(
                          accessToken = projectCache.get(build.project.id).postAccessKey,
                          environment = FlowEnvironment.Current.toString,
                          revision = tag.hash
                        )) onComplete {
                          case Success(_) => log.info(s"Rollbar: success posting deploy")
                          case Failure(e) => log.warn(s"Rollbar: Failed to post deploy", e)
                        }
                      } else {
                        log.warn("Rollbar: no project or access key exists in rollbar - cannot post deploy")
                      }
                    }
                  }
                }
            }
        }
      }

    case RollbarActor.Messages.Refresh =>
      accessToken.foreach { token =>
        rollbar.projects.getProjects(token).flatMap { projects =>
          Future.sequence(projects.result.flatMap { project =>
            project.name.map { projectName =>
              rollbar.projects.getProjectAndAccessTokensByProjectId(project.id, token).map { accessTokens =>
                accessTokens.result.find(_.scopes.contains("post_server_item")).foreach { token =>
                  projectCache.put(projectName, Project(projectName, project.id, token.accessToken))
                }
              }
            }
          })
        }.failed.foreach { ex =>
          logger.warn("failed to fetch rollbar projects", ex)
        }
      }
  }

}
