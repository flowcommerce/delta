package controllers

import io.flow.delta.actors.MainActor
import io.flow.postgresql.Authorization
import db.BuildsDao
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class AwsActors @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef
) extends Controller {

  def postByBuildId(buildId: String) = Action { request =>
    BuildsDao.findById(Authorization.All, buildId) match {
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
