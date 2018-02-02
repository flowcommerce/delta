package controllers

import db.HealthchecksDao
import io.flow.common.v0.models.Healthcheck
import io.flow.common.v0.models.json._
import io.flow.delta.aws.Credentials
import io.flow.play.util.Validation
import play.api.libs.json._
import play.api.mvc._
import io.flow.error.v0.models.json._

@javax.inject.Singleton
class Healthchecks @javax.inject.Inject() (
  credentials: Credentials,
  healthchecksDao: HealthchecksDao,
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  val controllerComponents: ControllerComponents
) extends BaseController {

  private val HealthyJson = Json.toJson(Healthcheck(status = "healthy"))

  def getHealthcheck() = Action { request =>
    val checks = Map(
      "db" -> healthchecksDao.isHealthy()
    )

    checks.filter { case (name, check) => !check }.keys.toList match {
      case Nil => Ok(HealthyJson)
      case unhealthy => UnprocessableEntity(Json.toJson(Validation.errors(unhealthy.map { name => s"$name failed check" })))
    }
  }

}
