package controllers

import io.flow.delta.www.lib.UiData
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import play.api.i18n._
import play.api.mvc._

class HealthchecksController @javax.inject.Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends FlowController with I18nSupport {

  def index() = Action { implicit request =>
    Ok(
      views.html.healthchecks.index(
        UiData(requestPath = request.path),
        "healthy"
      )
    )

  }

}
