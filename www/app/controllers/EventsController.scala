package controllers

import io.flow.delta.v0.models.EventType
import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.PaginatedCollection
import play.api.i18n.MessagesApi
import play.api.mvc.ControllerComponents

class EventsController @javax.inject.Inject() (
  override val messagesApi: MessagesApi,
  override val tokenClient: io.flow.token.v0.interfaces.Client,
  override val deltaClientProvider: DeltaClientProvider,
  override val controllerComponents: ControllerComponents,
  override val flowControllerComponents: FlowControllerComponents
) extends BaseController(tokenClient, deltaClientProvider, controllerComponents, flowControllerComponents) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] val Limit = 100

  override def section = Some(io.flow.delta.www.lib.Section.Events)

  def index(
    page: Int = 0,
    projectId: Option[String],
    `type`: Option[EventType],
    hasError: Option[Boolean]
  ) = Identified.async { implicit request =>
    for {
      events <- deltaClient(request).events.get(
        projectId = projectId,
        `type` = `type`,
        hasError = hasError,
        limit = Limit+1,
        offset = page * Limit
      )
    } yield {
      val title = projectId match {
        case None => {
          `type` match {
            case None => "Event Log"
            case Some(t) => s"Event Log: (type $t)"
          }
        }
        case Some(id) => {
          `type` match {
            case None => s"Event Log: $id"
            case Some(t) => s"Event Log: $id (type: $t)"
          }
        }
      }

      Ok(
        views.html.events.index(
          uiData(request).copy(title = Some(title)),
          projectId,
          `type`,
          hasError,
          PaginatedCollection(page, events, Limit)
        )
      )
    }
  }

}
