package controllers

import db.ItemsDao
import io.flow.delta.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class Items @javax.inject.Inject() (
  itemsDao: ItemsDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    q: Option[String],
    limit: Option[Long],
    offset: Long = 0
  ) = Identified { request =>
    Ok(
      Json.toJson(
        itemsDao.findAll(
          authorization(request),
          q = q,
          limit = limit,
          offset = offset
        )
      )
    )
  }

}
