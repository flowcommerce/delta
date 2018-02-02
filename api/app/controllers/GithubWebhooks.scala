package controllers

import db.ProjectsDao
import io.flow.delta.actors.MainActor
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import io.flow.postgresql.Authorization
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class GithubWebhooks @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  projectsDao: ProjectsDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends FlowController {

  def postByProjectId(projectId: String) = Action { request =>
    projectsDao.findById(Authorization.All, projectId) match {
      case None => {
        NotFound
      }
      case Some(project) => {
        play.api.Logger.info(s"Received github webhook for project[${project.id}] name[${project.name}]")
        mainActor ! MainActor.Messages.ProjectSync(project.id)
        Ok(Json.toJson(Map("result" -> "success")))
      }
    }
  }

}
