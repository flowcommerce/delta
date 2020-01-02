package controllers

import db.DashboardBuildsDao
import io.flow.delta.config.v0.models.Cluster
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
    organization: Option[String],
    cluster: Option[Cluster],
    limit: Long,
    offset: Long
  ) = Identified { request =>
    Ok(
      Json.toJson(
        dashboardBuildsDao.findAll(
          authorization(request),
          organization = organization,
          cluster=  cluster,
          limit = Some(limit),
          offset = offset
        )
      )
    )
  }

}
