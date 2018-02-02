package controllers

import io.flow.delta.www.lib.UiData
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.ControllerComponents

class LogoutController @javax.inject.Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends FlowController with I18nSupport {

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
