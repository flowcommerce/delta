package controllers

import db.{InternalTokenForm, TokensDao}
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.json._
import io.flow.delta.v0.models.{Token, TokenForm}
import io.flow.error.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.Validation
import io.flow.postgresql.Authorization
import play.api.libs.json._
import play.api.mvc._

class Tokens @javax.inject.Inject() (
  helpers: Helpers,
  tokensDao: TokensDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    userId: Option[String],
    limit: Option[Long],
    offset: Long,
    sort: String
  ) = Identified { request =>
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          tokensDao.findAll(
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
    withToken(request.user, id) { token =>
      Ok(Json.toJson(tokensDao.addCleartextIfAvailable(request.user, token)))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[TokenForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[TokenForm] => {
        tokensDao.create(request.user, InternalTokenForm.UserCreated(s.get)) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(token) => Created(Json.toJson(token))
        }
      }
    }
  }

  def deleteById(id: String) = Identified { request =>
    withToken(request.user, id) { token =>
      tokensDao.delete(request.user, token)
      NoContent
    }
  }

  def withToken(user: UserReference, id: String)(
    f: Token => Result
  ) = {
    tokensDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(token) => {
        f(token)
      }
    }
  }

}
