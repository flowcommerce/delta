package controllers

import io.flow.common.v0.models.{User, UserReference}
import io.flow.delta.v0.models.{Publication, SubscriptionForm}
import io.flow.delta.www.lib.{DeltaClientProvider, UiData}
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.Config
import play.api.i18n.I18nSupport
import play.api.mvc.{ControllerComponents, Request}

import scala.concurrent.{ExecutionContext, Future}

object Subscriptions {

  case class UserPublication(publication: Publication, isSubscribed: Boolean) {
    val label = publication match {
      case Publication.Deployments => "Email me whenever a new deployment is created"
      case Publication.UNDEFINED(key) => key
    }
  }

}

class SubscriptionsController @javax.inject.Inject() (
  val config: Config,
  deltaClientProvider: DeltaClientProvider,
  controllerComponents: ControllerComponents,
  flowControllerComponents: FlowControllerComponents
)(implicit ec: ExecutionContext)
  extends BaseController(deltaClientProvider, controllerComponents, flowControllerComponents) with I18nSupport {

  override def section = Some(io.flow.delta.www.lib.Section.Events)

  lazy val client = deltaClientProvider.newClient(user = None, requestId = None)

  def index() =  User.async { implicit request =>
    deltaClientProvider.newClient(user = Some(request.user), requestId = None).users.getIdentifierById(request.user.id).map { id =>
      Redirect(routes.SubscriptionsController.identifier(id.value))
    }
  }

  def identifier(identifier: String) = Action.async { implicit request =>
    for {
      users <- client.users.get(
        identifier = Some(identifier)
      )
      user <- Future { users.headOption.map(u => UserReference(u.id)) }
      subscriptions <- deltaClientProvider.newClient(user = user, requestId = None).subscriptions.get(
        identifier = Some(identifier),
        limit = Publication.all.size.toLong + 1L
      )
    } yield {
      val userPublications = Publication.all.map { p =>
        Subscriptions.UserPublication(
          publication = p,
          isSubscribed = !subscriptions.find(_.publication == p).isEmpty
            )
      }
      Ok(views.html.subscriptions.identifier(uiData(request, users.headOption), identifier, userPublications))
    }
  }

  def postToggle(identifier: String, publication: Publication) = Action.async {
    client.users.get(identifier = Some(identifier)).flatMap { users =>
      users.headOption match {
        case None => Future {
          Redirect(routes.SubscriptionsController.index()).flashing("warning" -> "User could not be found")
        }
        case Some(user) => {
          val identifiedClient = deltaClientProvider.newClient(user = Some(UserReference(user.id)), requestId = None)

          identifiedClient.subscriptions.get(
            identifier = Some(identifier),
            publication = Some(publication)
          ).flatMap { subscriptions =>
            subscriptions.headOption match {
              case None => {
                identifiedClient.subscriptions.post(
                  SubscriptionForm(
                    userId = user.id,
                    publication = publication
                  ),
                  identifier = Some(identifier)
                ).map { _ =>
                  Redirect(routes.SubscriptionsController.identifier(identifier)).flashing("success" -> "Subscription added")
                }
              }
              case Some(subscription) => {
                identifiedClient.subscriptions.deleteById(
                  subscription.id,
                  identifier = Some(identifier)
                ).map { _ =>
                  Redirect(routes.SubscriptionsController.identifier(identifier)).flashing("success" -> "Subscription removed")
                }
              }
            }
          }
        }
      }
    }
  }

  def uiData[T](request: Request[T], user: Option[User]): UiData = {
    UiData(
      requestPath = request.path,
      user = user,
      section = Some(io.flow.delta.www.lib.Section.Subscriptions)
    )
  }

}
