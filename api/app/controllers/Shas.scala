package controllers

import db.{ShasDao, ShasWriteDao}
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.Sha
import io.flow.delta.v0.models.json._
import io.flow.play.util.Config
import io.flow.postgresql.Authorization
import play.api.mvc._
import play.api.libs.json._

@javax.inject.Singleton
class Shas @javax.inject.Inject() (
  override val config: Config,
  override val tokenClient: io.flow.token.v0.interfaces.Client,
  shasWriteDao: ShasWriteDao
) extends Controller with BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    project: Option[String],
    branch: Option[String],
    hash: Option[String],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          ShasDao.findAll(
            authorization(request),
            ids = optionals(id),
            projectId = project,
            branch = branch,
            hash = hash,
            limit = limit,
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    withSha(request.user, id) { sha =>
      Ok(Json.toJson(sha))
    }
  }

  def deleteById(id: String) = Identified { request =>
    withSha(request.user, id) { sha =>
      shasWriteDao.delete(request.user, sha)
      NoContent
    }
  }

  def withSha(user: UserReference, id: String)(
    f: Sha => Result
  ): Result = {
    ShasDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(sha) => {
        f(sha)
      }
    }
  }

  
}
