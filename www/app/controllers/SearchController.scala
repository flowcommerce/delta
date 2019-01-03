package controllers

import io.flow.delta.lib.Urls
import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.{Config, PaginatedCollection, Pagination}
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class SearchController @javax.inject.Inject() (
  val config: Config,
  deltaClientProvider: DeltaClientProvider,
  controllerComponents: ControllerComponents,
  flowControllerComponents: FlowControllerComponents,
  urls: Urls,
)(implicit ec: ExecutionContext)
  extends BaseController(deltaClientProvider, controllerComponents, flowControllerComponents) {

  override def section = None

  def index(
    q: Option[String],
    page: Int
  ) = User.async { implicit request =>
    for {
      items <- deltaClient(request).items.get(
        q = q,
        limit = Pagination.DefaultLimit.toLong + 1L,
        offset = page * Pagination.DefaultLimit.toLong
      )
    } yield {
      Ok(
        views.html.search.index(
          uiData(request).copy(
            title = q.map { v => s"Search results for $v" },
            query = q
          ),
          urls,
          q,
          PaginatedCollection(page, items)
        )
      )
    }
  }

}
