package controllers

import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.{Config, PaginatedCollection, Pagination}
import play.api.i18n.MessagesApi
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class SearchController @javax.inject.Inject() (
  val config: Config,
  messagesApi: MessagesApi,
  deltaClientProvider: DeltaClientProvider,
  controllerComponents: ControllerComponents,
  flowControllerComponents: FlowControllerComponents
)(implicit ec: ExecutionContext)
  extends BaseController(deltaClientProvider, controllerComponents, flowControllerComponents) {

  override def section = None

  def index(
    q: Option[String],
    page: Int
  ) = IdentifiedCookie.async { implicit request =>
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
