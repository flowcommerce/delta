package actors

import akka.actor.{Actor, ActorSystem}
import db.{BuildsDao, TagsDao}
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.v0.models.{Build, State}
import io.flow.play.actors.{ErrorHandler, Scheduler}
import io.flow.play.util.{Config, FlowEnvironment}
import io.flow.postgresql.Authorization
import io.flow.rollbar.v0.models.Deploy
import javax.inject.{Inject, Singleton}
import io.flow.rollbar.v0.{Client => Rollbar}
import play.api.Logger
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class Deployment(buildId: String, diffs: Seq[StateDiff])

case object Refresh

@Singleton
class RollbarActor @Inject()(
  ws: WSClient,
  system: ActorSystem,
  buildsDao: BuildsDao,
  tagsDao: TagsDao,
  val config: Config
) extends Actor with ErrorHandler with Scheduler {

  private implicit val ec = system.dispatchers.lookup("rollbar-actor-context")

  private val rollbar = new Rollbar(ws)

  private val logger = Logger(getClass)

  private val accessToken = config.requiredString("rollbar.access_token")

  private case class Project(name: String, id: Int, postAccessKey: String)

  private val projectCache = new java.util.concurrent.ConcurrentHashMap[String, Project]()

  scheduleRecurring(system, "rollbar.actor.refresh") {
    self ! Refresh
  }

  def receive = akka.event.LoggingReceive {

    case msg @ Deployment(buildId, diffs) => withErrorHandler(msg) {

      buildsDao.findById(Authorization.All, buildId) match {
        case None => throw new IllegalArgumentException(buildId)
        case Some(build) =>
          diffs.headOption match { // do we ever have more than 1 statediff?
            case None => throw new IllegalArgumentException(diffs.toString)
            case Some(diff) =>

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
                      case Failure(e) => logger.error(s"failed to post deploy $msg")
                    }
                  } else {
                    logger.warn(s"no project or access key for `${build.project.id}' exist in rollbar")
                  }
              }
          }
      }
    }

    case msg @ Refresh => withErrorHandler(msg) {

      rollbar.projects.getProjects(accessToken).flatMap { projects =>
        Future.sequence(projects.result.map { project =>
          rollbar.projects.getProjectAndAccessTokensByProjectId(project.id, accessToken).map { accessTokens =>
            accessTokens.result.find(_.scopes.contains("post_server_item")).foreach { token =>
              projectCache.put(project.name, Project(project.name, project.id, token.accessToken))
            }
          }
        })
      }

    }


  }

}
