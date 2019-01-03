package controllers

import db.HealthchecksDao
import io.flow.error.v0.models.json._
import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.healthcheck.v0.models.json._
import io.flow.play.util.Validation
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._

@Singleton
class Healthchecks @Inject() (
  healthchecksDao: HealthchecksDao,
  val controllerComponents: ControllerComponents
) extends BaseController {

  private val HealthyJson = Json.toJson(Healthcheck(status = "healthy"))

  def getHealthcheck() = Action {
    val checks = Map(
      "db" -> healthchecksDao.isHealthy()
    )

    checks.filter { case (_, check) => !check }.keys.toList match {
      case Nil => Ok(HealthyJson)
      case unhealthy => UnprocessableEntity(Json.toJson(Validation.errors(unhealthy.map { name => s"$name failed check" })))
    }
  }

}
