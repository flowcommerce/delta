package controllers

import io.flow.delta.www.lib.UiData
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class LogoutController @javax.inject.Inject() (
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  def logged_out = Action { implicit request =>
    Ok(
      views.html.logged_out(
        UiData(requestPath = request.path)
      )
    )
  }

  def index() = Action {
    Redirect("/logged_out").withNewSession
  }


}
