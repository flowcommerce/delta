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

import scala.concurrent.Future
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

  private implicit val ec = system.dispatchers.lookup("rollbar-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("RollbarActor")

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

    case msg @ RollbarActor.Messages.Deployment(buildId, diffs) =>
      accessToken.foreach { _ =>
        buildsDao.findById(Authorization.All, buildId) match {
          case None => throw new IllegalArgumentException(buildId)
          case Some(build) =>
            diffs.headOption match { // do we ever have more than 1 statediff?
              case None => throw new IllegalArgumentException(diffs.toString)
              case Some(diff) =>

                // log to determine whether we should only post when lastInstances == 0
                configuredRollbar.withKeyValue("config", ConfigName).withKeyValue("project", build.project.id).withKeyValue("diffs", diffs.map(_.toString)).warn(s"got deploy")

                if (diff.desiredInstances > diff.lastInstances) { // scale up
                  tagsDao.findByProjectIdAndName(Authorization.All, build.project.id, diff.versionName) match {
                    case None =>
                    case Some(tag) =>

                      if (projectCache.containsKey(build.project.id)) {
                        rollbar.deploys.post(Deploy(
                          accessToken = projectCache.get(build.project.id).postAccessKey,
                          environment = FlowEnvironment.Current.toString,
                          revision = tag.hash
                        )) onComplete {
                          case Success(_) => logger.info(s"success posting $msg")
                          case Failure(e) => logger.error(s"failed to post deploy $msg", e)
                        }
                      } else {
                        configuredRollbar.withKeyValue("config", ConfigName).withKeyValue("project", build.project.id).warn(s"no project or access key for project exist in rollbar")
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
