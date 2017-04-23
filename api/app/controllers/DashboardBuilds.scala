package controllers

import db.DashboardBuildsDao
import io.flow.delta.v0.models.json._
import io.flow.play.util.Config
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class DashboardBuilds @javax.inject.Inject() (
  override val config: Config,
) extends Controller with BaseFlowController {

  def get(
    limit: Long,
    offset: Long
  ) = Identified { request =>
    Ok(
      Json.toJson(
        DashboardBuildsDao.findAll(
          authorization(request),
          limit = limit,
          offset = offset
        )
      )
    )
  }

}
