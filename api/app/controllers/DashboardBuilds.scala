package controllers

import db.DashboardBuildsDao
import io.flow.delta.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class DashboardBuilds @javax.inject.Inject() (
  dashboardBuildsDao: DashboardBuildsDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    limit: Long,
    offset: Long
  ) = Identified { request =>
    Ok(
      Json.toJson(
        dashboardBuildsDao.findAll(
          authorization(request),
          limit = limit,
          offset = offset
        )
      )
    )
  }

}
