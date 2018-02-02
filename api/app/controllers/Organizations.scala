package controllers

import db.{OrganizationsDao, OrganizationsWriteDao}
import io.flow.play.util.{Config, Validation}
import io.flow.delta.v0.models.OrganizationForm
import io.flow.delta.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.mvc._
import play.api.libs.json._

class Organizations @javax.inject.Inject() (
  override val config: Config,
  override val tokenClient: io.flow.token.v0.interfaces.Client,
  organizationsWriteDao: OrganizationsWriteDao
) extends Controller with BaseIdentifiedRestController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    id: Option[Seq[String]],
    userId: Option[String],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          OrganizationsDao.findAll(
            authorization(request),
            ids = optionals(id),
            userId = userId,
            limit = limit,
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    withOrganization(request.user, id) { organization =>
      Ok(Json.toJson(organization))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[OrganizationForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[OrganizationForm] => {
        organizationsWriteDao.create(request.user, s.get) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(organization) => Created(Json.toJson(organization))
        }
      }
    }
  }

  def putById(id: String) = Identified(parse.json) { request =>
    withOrganization(request.user, id) { organization =>
      request.body.validate[OrganizationForm] match {
        case e: JsError => {
          UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[OrganizationForm] => {
          organizationsWriteDao.update(request.user, organization, s.get) match {
            case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
            case Right(updated) => Ok(Json.toJson(updated))
          }
        }
      }
    }
  }

  def deleteById(id: String) = Identified { request =>
    withOrganization(request.user, id) { organization =>
      organizationsWriteDao.delete(request.user, organization)
      NoContent
    }
  }
}
