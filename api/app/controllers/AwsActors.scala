package controllers

import db.BuildsDao
import io.flow.delta.actors.MainActor
import io.flow.play.controllers.FlowControllerComponents
import io.flow.postgresql.Authorization
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class AwsActors @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  buildsDao: BuildsDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def postByBuildId(buildId: String) = Action { request =>
    buildsDao.findById(Authorization.All, buildId) match {
      case None => {
        NotFound
      }
      case Some(build) => {
        play.api.Logger.info(s"Received request for build[${build.id}] name[${build.name}]")
        mainActor ! MainActor.Messages.ConfigureAWS(build.id)
        Ok(Json.toJson(Map("result" -> "success")))
      }
    }
  }

}
