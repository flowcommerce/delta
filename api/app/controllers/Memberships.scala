package controllers

import db.MembershipsDao
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.json._
import io.flow.delta.v0.models.{Membership, MembershipForm, Role}
import io.flow.error.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.Validation
import io.flow.postgresql.Authorization
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class Memberships @javax.inject.Inject() (
  helpers: Helpers,
  membershipsDao: MembershipsDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    organization: Option[String],
    userId: Option[String],
    role: Option[Role],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          membershipsDao.findAll(
            authorization(request),
            ids = optionals(id),
            organizationId = organization,
            userId = userId,
            role = role,
            limit = Some(limit),
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    withMembership(request.user, id) { membership =>
      Ok(Json.toJson(membership))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[MembershipForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[MembershipForm] => {
        membershipsDao.create(request.user, s.get) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(membership) => Created(Json.toJson(membership))
        }
      }
    }
  }

  def deleteById(id: String) = Identified { request =>
    withMembership(request.user, id) { membership =>
      membershipsDao.delete(request.user, membership)
      NoContent
    }
  }

  def withMembership(user: UserReference, id: String)(
    f: Membership => Result
  ): Result = {
    membershipsDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(membership) => {
        f(membership)
      }
    }
  }

}
