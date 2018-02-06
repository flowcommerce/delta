package controllers

import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.GithubAuthenticationForm
import io.flow.delta.www.lib.{DeltaClientProvider, UiData}
import io.flow.play.controllers.IdentifiedCookie._
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import play.api.Logger
import play.api.i18n._
import play.api.mvc.ControllerComponents

class LoginController @javax.inject.Inject() (
  override val messagesApi: MessagesApi,
  val provider: DeltaClientProvider,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends FlowController with I18nSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(returnUrl: Option[String]) = Action { implicit request =>
    Ok(views.html.login.index(UiData(requestPath = request.path), returnUrl))
  }

  def githubCallback(
    code: String,
    state: Option[String],
    returnUrl: Option[String]
  ) = Action.async { implicit request =>
    provider.newClient(user = None, requestId = None).githubUsers.postGithub(
      GithubAuthenticationForm(
        code = code
      )
    ).map { user =>
      val url = returnUrl match {
        case None => {
          routes.ApplicationController.index().path
        }
        case Some(u) => {
          assert(u.startsWith("/"), s"Redirect URL[$u] must start with /")
          u
        }
      }
      Logger.info(s"Redirecting to url [$url]")
      Redirect(url).withIdentifiedCookieUser(UserReference(user.id.toString))
    }.recover {
      case response: io.flow.delta.v0.errors.GenericErrorResponse => {
        Ok(views.html.login.index(UiData(requestPath = request.path), returnUrl, response.genericError.messages))
      }

      case ex: Throwable => sys.error(s"Github callback failed to authenticate user.")
    }
  }

}
