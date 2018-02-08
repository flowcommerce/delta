package controllers

import io.flow.delta.v0.errors.UnitResponse
import io.flow.delta.v0.models.{Token, TokenForm}
import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.controllers.{FlowControllerComponents, IdentifiedRequest}
import io.flow.play.util.{Config, PaginatedCollection, Pagination}

import scala.concurrent.{ExecutionContext, Future}
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

class TokensController @javax.inject.Inject() (
  val config: Config,
  messagesApi: MessagesApi,
  deltaClientProvider: DeltaClientProvider,
  controllerComponents: ControllerComponents,
  flowControllerComponents: FlowControllerComponents
)(implicit ec: ExecutionContext)
  extends BaseController(deltaClientProvider, controllerComponents, flowControllerComponents) {

  override def section = None

  def index(page: Int = 0) = IdentifiedCookie.async { implicit request =>
    for {
      tokens <- deltaClient(request).tokens.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.tokens.index(uiData(request), PaginatedCollection(page, tokens)))
    }
  }

  def show(id: String) = IdentifiedCookie.async { implicit request =>
    withToken(request, id) { token =>
      Future {
        Ok(views.html.tokens.show(uiData(request), token))
      }
    }
  }

  def create() = IdentifiedCookie { implicit request =>
    Ok(views.html.tokens.create(uiData(request), TokensController.tokenForm))
  }

  def postCreate = IdentifiedCookie.async { implicit request =>
    val form = TokensController.tokenForm.bindFromRequest
    form.fold (

      errors => Future {
        Ok(views.html.tokens.create(uiData(request), errors))
      },

      valid => {
        deltaClient(request).tokens.post(
          TokenForm(
            userId = request.user.id,
            description = valid.description
          )
        ).map { token =>
          Redirect(routes.TokensController.show(token.id)).flashing("success" -> "Token created")
        }.recover {
          case r: io.flow.delta.v0.errors.GenericErrorResponse => {
            Ok(views.html.tokens.create(uiData(request), form, r.genericError.messages))
          }
        }
      }

    )
  }

  def postDelete(id: String) = IdentifiedCookie.async { implicit request =>
    deltaClient(request).tokens.deleteById(id).map { response =>
      Redirect(routes.TokensController.index()).flashing("success" -> s"Token deleted")
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.TokensController.index()).flashing("warning" -> s"Token not found")
      }
    }
  }

  def withToken[T](
    request: IdentifiedRequest[T],
    id: String
  )(
    f: Token => Future[Result]
  ) = {
    deltaClient(request).tokens.getById(id).flatMap { token =>
      f(token)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.TokensController.index()).flashing("warning" -> s"Token not found")
      }
    }
  }

}

object TokensController {

  case class TokenData(
    description: Option[String]
  )

  private[controllers] val tokenForm = Form(
    mapping(
      "description" -> optional(nonEmptyText)
    )(TokenData.apply)(TokenData.unapply)
  )
}
