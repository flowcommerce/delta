package controllers

import com.github.ghik.silencer.silent
import db.SubscriptionsDao
import io.flow.delta.v0.models.json._
import io.flow.delta.v0.models.{Publication, Subscription, SubscriptionForm}
import io.flow.error.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.Validation
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class Subscriptions @javax.inject.Inject() (
  helpers: Helpers,
  subscriptionsDao: SubscriptionsDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    userId: Option[String],
    identifier: Option[String],
    publication: Option[Publication],
    limit: Option[Long],
    offset: Long,
    sort: String
  ) = Identified {
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          subscriptionsDao.findAll(
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

  def getById(id: String) = Identified {
    withSubscription(id) { subscription =>
      Ok(Json.toJson(subscription))
    }
  }

  @silent
  def post(identifier: Option[String]) = Identified(parse.json) { request =>
    request.body.validate[SubscriptionForm] match {
      case e: JsError => {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[SubscriptionForm] => {
        val form = s.get
        subscriptionsDao.create(request.user, form) match {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(subscription) => Created(Json.toJson(subscription))
        }
      }
    }
  }

  @silent
  def deleteById(id: String, identifier: Option[String]) = Identified { request =>
    withSubscription(id) { subscription =>
      subscriptionsDao.delete(request.user, subscription)
      NoContent
    }
  }

  def withSubscription(id: String)(
    f: Subscription => Result
  ): Result = {
    subscriptionsDao.findById(id) match {
      case None => {
        NotFound
      }
      case Some(subscription) => {
        f(subscription)
      }
    }
  }

}
