package controllers

import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.util.{Pagination, PaginatedCollection}
import play.api.i18n.MessagesApi

class SearchController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val deltaClientProvider: DeltaClientProvider
) extends BaseController(tokenClient, deltaClientProvider) {

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
