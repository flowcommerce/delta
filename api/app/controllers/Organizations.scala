package controllers

import db.{OrganizationsDao, OrganizationsWriteDao}
import io.flow.delta.v0.models.OrganizationForm
import io.flow.delta.v0.models.json._
import io.flow.error.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.Validation
import play.api.libs.json._
import play.api.mvc._

class Organizations @javax.inject.Inject() (
  helpers: Helpers,
  organizationsDao: OrganizationsDao,
  organizationsWriteDao: OrganizationsWriteDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    userId: Option[String],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          organizationsDao.findAll(
            authorization(request),
            ids = optionals(id),
            userId = userId,
            limit = Some(limit),
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    helpers.withOrganization(request.user, id) { organization =>
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
    helpers.withOrganization(request.user, id) { organization =>
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
    helpers.withOrganization(request.user, id) { organization =>
      organizationsWriteDao.delete(request.user, organization)
      NoContent
    }
  }
}
