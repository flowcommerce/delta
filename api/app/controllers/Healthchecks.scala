package controllers

import db.HealthchecksDao
import io.flow.delta.aws.Credentials
import io.flow.error.v0.models.json._
import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.models.json._
import io.flow.play.util.Validation
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._

@Singleton
class Healthchecks @Inject() (
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
