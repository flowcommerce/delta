package controllers

import db.{TagsDao, TagsWriteDao}
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.Tag
import io.flow.delta.v0.models.json._
import io.flow.play.util.Config
import io.flow.postgresql.Authorization
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class Tags @javax.inject.Inject() (
  override val config: Config,
  tagsWriteDao: TagsWriteDao  
) extends Controller with BaseFlowController {

  def get(
    id: Option[Seq[String]],
    project: Option[String],
    name: Option[String],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          TagsDao.findAll(
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
    TagsDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(tag) => {
        f(tag)
      }
    }
  }

}
