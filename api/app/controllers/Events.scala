package controllers

import db.EventsDao
import io.flow.delta.v0.models.{Event, EventType}
import io.flow.delta.v0.models.json._
import io.flow.play.util.Config
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class Events @javax.inject.Inject() (
  override val config: Config,
  override val tokenClient: io.flow.token.v0.interfaces.Client
) extends Controller with BaseIdentifiedRestController {

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
    withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          EventsDao.findAll(
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
    EventsDao.findById(id) match {
      case None => {
        Results.NotFound
      }
      case Some(event) => {
        f(event)
      }
    }
  }

}  
