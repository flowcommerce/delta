package controllers

import db.{UserIdentifiersDao, UsersDao, UsersWriteDao}
import io.flow.common.v0.models.json._
import io.flow.common.v0.models.{User, UserReference}
import io.flow.delta.v0.models.UserForm
import io.flow.delta.v0.models.json._
import io.flow.error.v0.models.json._
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import io.flow.play.util.Validation
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class Users @javax.inject.Inject() (
  usersDao: UsersDao,
  userIdentifiersDao: UserIdentifiersDao,
  usersWriteDao: UsersWriteDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents,
  implicit val ec: ExecutionContext
) extends FlowController {
  def get(
    id: Option[String],
    email: Option[String],
    identifier: Option[String]
  ) = Anonymous { _ =>

    if (Seq(id, email, identifier).flatten.isEmpty) {
      UnprocessableEntity(Json.toJson(Validation.error("Must specify id, email or identifier")))
    } else {
      Ok(
        Json.toJson(
          usersDao.findAll(
            id = id,
            email = email,
            identifier = identifier,
            limit = Some(1),
            offset = 0
          )
        )
      )
    }
  }

  def getById(id: String) = Identified {
    withUser(id) { user =>
      Ok(Json.toJson(user))
    }
  }

  def getIdentifierById(id: String) = Identified { request =>
    withUser(id) { user =>
      Ok(Json.toJson(userIdentifiersDao.latestForUser(request.user, UserReference(id = user.id))))
    }
  }

  def post() = Anonymous.async(parse.json) { request =>
    request.body.validate[UserForm] match {
      case e: JsError => Future {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[UserForm] => {
        Future {
          usersWriteDao.create(request.user, s.get) match {
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
    usersDao.findById(id) match {
      case None => {
        NotFound
      }
      case Some(user) => {
        f(user)
      }
    }
  }

}
