package controllers

import io.flow.delta.www.lib.UiData
import io.flow.common.v0.models.Healthcheck
import io.flow.common.v0.models.json._

import play.api._
import play.api.i18n._
import play.api.mvc._

class HealthchecksController @javax.inject.Inject() (
  val messagesApi: MessagesApi
) extends Controller with I18nSupport {

  def index() = Action { implicit request =>
    Ok(
      views.html.healthchecks.index(
        UiData(requestPath = request.path),
        "healthy"
      )
    )

  }

}
