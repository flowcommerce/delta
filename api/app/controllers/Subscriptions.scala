package controllers

import db.{SubscriptionsDao, UsersDao}
import io.flow.play.util.{Config, Validation}
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.{Publication, Subscription, SubscriptionForm}
import io.flow.delta.v0.models.json._
import io.flow.common.v0.models.json._
import play.api.Logger
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class Subscriptions @javax.inject.Inject() (
  override val config: Config,
) extends Controller with BaseFlowController {

  /**
   * If we find an 'identifier' query string parameter, use that to
   * find the user and authenticate as that user.
   */
  override def user(
    session: Session,
    headers: Headers,
    path: String,
    queryString: Map[String, Seq[String]]
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[UserReference]] = {
    queryString.get("identifier").getOrElse(Nil).toList match {
      case Nil => {
        super.user(session, headers, path, queryString)
      }
      case id :: Nil => {
        Future {
          UsersDao.findAll(identifier = Some(id), limit = 1).headOption.map { u =>
            UserReference(id = u.id)
          }
        }
      }
      case inple => {
        Logger.warn(s"Multiple identifiers[${inple.size}] found in request - assuming no User")
        Future { None }
      }
    }
  }

  def get(
    id: Option[Seq[String]],
    userId: Option[String],
    identifier: Option[String],
    publication: Option[Publication],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          SubscriptionsDao.findAll(
            ids = optionals(id),
            userId = userId,
            identifier = identifier,
            publication = publication,
            limit = limit,
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    withSubscription(id) { subscription =>
      Ok(Json.toJson(subscription))
    }
  }

  def post(identifier: Option[String]) = Identified(parse.json) { request =>
    request.body.validate[SubscriptionForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[SubscriptionForm] => {
        val form = s.get
        SubscriptionsDao.create(request.user, form) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(subscription) => Created(Json.toJson(subscription))
        }
      }
    }
  }

  def deleteById(id: String, identifier: Option[String]) = Identified { request =>
    withSubscription(id) { subscription =>
      SubscriptionsDao.delete(request.user, subscription)
      NoContent
    }
  }

  def withSubscription(id: String)(
    f: Subscription => Result
  ): Result = {
    SubscriptionsDao.findById(id) match {
      case None => {
        NotFound
      }
      case Some(subscription) => {
        f(subscription)
      }
    }
  }

}
