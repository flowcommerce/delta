package controllers

import io.flow.delta.v0.models.{Publication, SubscriptionForm}
import io.flow.delta.www.lib.{DeltaClientProvider, UiData}
import io.flow.common.v0.models.User
import scala.concurrent.Future

import play.api._
import play.api.i18n._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Subscriptions {

  case class UserPublication(publication: Publication, isSubscribed: Boolean) {
    val label = publication match {
      case Publication.Deployments => "Email me whenever a new deployment is created"
      case Publication.UNDEFINED(key) => key
    }
  }

}

class SubscriptionsController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val tokenClient: io.flow.token.v0.interfaces.Client,
  val deltaClientProvider: DeltaClientProvider
) extends Controller
    with I18nSupport
{

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val client = deltaClientProvider.newClient(user = None)

  def index() = Action.async { implicit request =>
    Helpers.userFromSession(tokenClient, request.session).flatMap { userOption =>
      userOption match {
        case None => Future {
          Redirect(routes.LoginController.index(return_url = Some(request.path)))
        }
        case Some(user) => {
          deltaClientProvider.newClient(user = Some(user)).users.getIdentifierById(user.id).map { id =>
            Redirect(routes.SubscriptionsController.identifier(id.value))
          }
        }
      }
    }
  }

  def identifier(identifier: String) = Action.async { implicit request =>
    for {
      users <- client.users.get(
        identifier = Some(identifier)
      )
      subscriptions <- client.subscriptions.get(
        identifier = Some(identifier),
        limit = Publication.all.size + 1
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

  def postToggle(identifier: String, publication: Publication) = Action.async { implicit request =>
    client.users.get(identifier = Some(identifier)).flatMap { users =>
      users.headOption match {
        case None => Future {
          Redirect(routes.SubscriptionsController.index()).flashing("warning" -> "User could not be found")
        }
        case Some(user) => {
          client.subscriptions.get(
            identifier = Some(identifier),
            publication = Some(publication)
          ).flatMap { subscriptions =>
            subscriptions.headOption match {
              case None => {
                client.subscriptions.post(
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
                client.subscriptions.deleteById(
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
