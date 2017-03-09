package controllers

import db.{InternalTokenForm, TokensDao}
import io.flow.delta.v0.models.{Token, TokenForm}
import io.flow.delta.v0.models.json._
import io.flow.common.v0.models.UserReference
import io.flow.common.v0.models.json._
import io.flow.play.util.{Config, Validation}
import io.flow.postgresql.Authorization
import play.api.mvc._
import play.api.libs.json._

class Tokens @javax.inject.Inject() (
  override val config: Config,
) extends Controller with BaseFlowController {

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
          TokensDao.findAll(
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
      Ok(Json.toJson(TokensDao.addCleartextIfAvailable(request.user, token)))
    }
  }

  def post() = Identified(parse.json) { request =>
    request.body.validate[TokenForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[TokenForm] => {
        TokensDao.create(request.user, InternalTokenForm.UserCreated(s.get)) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(token) => Created(Json.toJson(token))
        }
      }
    }
  }

  def deleteById(id: String) = Identified { request =>
    withToken(request.user, id) { token =>
      TokensDao.delete(request.user, token)
      NoContent
    }
  }

  def withToken(user: UserReference, id: String)(
    f: Token => Result
  ) = {
    TokensDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(token) => {
        f(token)
      }
    }
  }

}
