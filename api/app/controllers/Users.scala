package controllers

import io.flow.delta.v0.models.UserForm
import io.flow.delta.v0.models.json._
import db.{UserIdentifiersDao, UsersDao, UsersWriteDao}
import io.flow.common.v0.models.{User, UserReference}
import io.flow.common.v0.models.json._
import io.flow.play.controllers.FlowController
import io.flow.play.util.{Config, Validation}
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future

class Users @javax.inject.Inject() (
  override val config: Config,
  usersWriteDao: UsersWriteDao
) extends Controller with FlowController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    id: Option[String],
    email: Option[String],
    identifier: Option[String]
  ) = Anonymous { request =>
    if (Seq(id, email, identifier).isEmpty) {
      UnprocessableEntity(Json.toJson(Validation.error("Must specify id, email or identifier")))
    } else {
      Ok(
        Json.toJson(
          UsersDao.findAll(
            id = id,
            email = email,
            identifier = identifier,
            limit = 1,
            offset = 0
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    withUser(id) { user =>
      Ok(Json.toJson(user))
    }
  }

  def getIdentifierById(id: String) = Identified { request =>
    withUser(id) { user =>
      Ok(Json.toJson(UserIdentifiersDao.latestForUser(request.user, UserReference(id = user.id))))
    }
  }

  def post() = Anonymous.async(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => Future {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserForm] => {
        request.user.map { userOption =>
          usersWriteDao.create(userOption, s.get) match {
            case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
            case Right(user) => Created(Json.toJson(user))
          }
        }
      }
    }
  }

  def withUser(id: String)(
    f: User => Result
  ) = {
    UsersDao.findById(id) match {
      case None => {
        NotFound
      }
      case Some(user) => {
        f(user)
      }
    }
  }

}
