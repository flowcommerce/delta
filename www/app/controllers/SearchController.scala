package controllers

import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.{PaginatedCollection, Pagination}
import play.api.i18n.MessagesApi
import play.api.mvc.ControllerComponents

class SearchController @javax.inject.Inject() (
  override val messagesApi: MessagesApi,
  override val tokenClient: io.flow.token.v0.interfaces.Client,
  override val deltaClientProvider: DeltaClientProvider,
  override val controllerComponents: ControllerComponents,
  override val flowControllerComponents: FlowControllerComponents
) extends BaseController(tokenClient, deltaClientProvider, controllerComponents, flowControllerComponents) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def section = None

  def index(
    q: Option[String],
    page: Int
  ) = Identified.async { implicit request =>
    for {
      items <- deltaClient(request).items.get(
        q = q,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.search.index(
          uiData(request).copy(
            title = q.map { v => s"Search results for $v" },
            query = q
          ),
          q,
          PaginatedCollection(page, items)
        )
      )
    }
  }

}
