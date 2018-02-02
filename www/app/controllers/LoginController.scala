package controllers

import io.flow.delta.www.lib.{DeltaClientProvider, UiData}
import io.flow.delta.v0.models.GithubAuthenticationForm
import play.api._
import play.api.i18n._
import play.api.mvc._

class LoginController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  val provider: DeltaClientProvider
) extends Controller
    with I18nSupport
{

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(returnUrl: Option[String]) = Action { implicit request =>
    Ok(views.html.login.index(UiData(requestPath = request.path), returnUrl))
  }

  def githubCallback(
    code: String,
    state: Option[String],
    returnUrl: Option[String]
  ) = Action.async { implicit request =>
    provider.newClient(None).githubUsers.postGithub(
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
      Redirect(url).withSession { "user_id" -> user.id.toString }
    }.recover {
      case response: io.flow.delta.v0.errors.ErrorsResponse => {
        Ok(views.html.login.index(UiData(requestPath = request.path), returnUrl, response.errors.map(_.message)))
      }
    }
  }

}
