package controllers

import com.github.ghik.silencer.silent
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.GithubAuthenticationForm
import io.flow.delta.www.lib.{Config, DeltaClientProvider, UiData}
import io.flow.play.controllers.IdentifiedCookie._
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import play.api.i18n._
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class LoginController @javax.inject.Inject() (
  override val messagesApi: MessagesApi,
  val provider: DeltaClientProvider,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents,
  config: Config,
)(implicit ec: ExecutionContext) extends FlowController with I18nSupport {

  def index(returnUrl: Option[String]) = Action { implicit request =>
    Ok(views.html.login.index(config, UiData(requestPath = request.path), returnUrl))
  }

  @silent
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
      Redirect(url).withIdentifiedCookieUser(UserReference(user.id.toString))
    }.recover {
      case response: io.flow.delta.v0.errors.GenericErrorResponse => {
        Ok(views.html.login.index(config, UiData(requestPath = request.path), returnUrl, response.genericError.messages))
      }

      case _: Throwable => sys.error(s"Github callback failed to authenticate user.")
    }
  }

}
