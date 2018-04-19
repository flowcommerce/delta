package actors

import akka.actor.Actor
import io.flow.play.actors.ErrorHandler
import io.flow.play.util.{Config, FlowEnvironment}
import io.flow.rollbar.v0.models.Deploy
import javax.inject.{Inject, Singleton}
import io.flow.rollbar.v0.{Client => Rollbar}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

case class Deployment(project: String, revision: String, user: String)

@Singleton
class RollbarActor @Inject()(
  ws: WSClient,
  config: Config
) extends Actor with ErrorHandler {

  private val rollbar = new Rollbar(ws)

  private val accessToken = config.requiredString("rollbar.access_token")

  private case class Project(name: String, id: Int, postAccessKey: String)

  private val projectCache = new java.util.concurrent.ConcurrentHashMap[String, Project]()

  def refresh() = {
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

  def receive = {

    case msg @ Deployment(project, revision, user) =>

      if (projectCache.containsKey(project)) {

        rollbar.deploys.post(Deploy(
          accessToken = projectCache.get(project).postAccessKey,
          environment = FlowEnvironment.Current.toString,
          revision = revision,
          localUsername = Some(user)
        ))

      } else {

        refresh.onComplete { _ =>
          self ! msg
        }

      }

  }

}
