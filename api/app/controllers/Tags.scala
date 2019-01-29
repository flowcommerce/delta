package controllers

import db.{TagsDao, TagsWriteDao}
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.Tag
import io.flow.delta.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import io.flow.postgresql.Authorization
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class Tags @javax.inject.Inject() (
  helpers: Helpers,
  tagsDao: TagsDao,
  tagsWriteDao: TagsWriteDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    project: Option[String],
    name: Option[String],
    limit: Option[Long],
    offset: Long,
    sort: String
  ) = Identified { request =>
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          tagsDao.findAll(
            authorization(request),
            ids = optionals(id),
            projectId = project,
            name = name,
            limit = limit,
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    withTag(request.user, id) { tag =>
      Ok(Json.toJson(tag))
    }
  }

  def deleteById(id: String) = Identified { request =>
    withTag(request.user, id) { tag =>
      tagsWriteDao.delete(request.user, tag)
      NoContent
    }
  }

  def withTag(user: UserReference, id: String)(
    f: Tag => Result
  ): Result = {
    tagsDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(tag) => {
        f(tag)
      }
    }
  }

}
