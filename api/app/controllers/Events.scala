package controllers

import db.EventsDao
import io.flow.delta.v0.models.json._
import io.flow.delta.v0.models.{Event, EventType}
import io.flow.play.controllers.FlowControllerComponents
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class Events @javax.inject.Inject() (
  eventsDao: EventsDao,
  helpers: Helpers,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    project: Option[String],
    `type`: Option[EventType],
    numberMinutesSinceCreation: Option[Long],
    hasError: Option[Boolean],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          eventsDao.findAll(
            ids = optionals(id),
            projectId = project,
            `type` = `type`,
            numberMinutesSinceCreation = numberMinutesSinceCreation,
            hasError = hasError,
            limit = limit,
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    withEvent(id) { event =>
      Ok(Json.toJson(event))
    }
  }

  def withEvent(id: String)(
    f: Event => Result
  ): Result = {
    eventsDao.findById(id) match {
      case None => {
        Results.NotFound
      }
      case Some(event) => {
        f(event)
      }
    }
  }

}  
