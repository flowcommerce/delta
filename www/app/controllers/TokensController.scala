package controllers

import io.flow.delta.v0.errors.UnitResponse
import io.flow.delta.v0.models.{Token, TokenForm}
import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.controllers.{FlowControllerComponents, IdentifiedRequest}
import io.flow.play.util.{Config, PaginatedCollection, Pagination}

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

class TokensController @javax.inject.Inject() (
  val config: Config,
  deltaClientProvider: DeltaClientProvider,
  controllerComponents: ControllerComponents,
  flowControllerComponents: FlowControllerComponents
)(implicit ec: ExecutionContext)
  extends controllers.BaseController(deltaClientProvider, controllerComponents, flowControllerComponents) {

  override def section = None

  def index(page: Int = 0) = User.async { implicit request =>
    for {
      tokens <- deltaClient(request).tokens.get(
        limit = Pagination.DefaultLimit.toLong + 1L,
        offset = page * Pagination.DefaultLimit.toLong
      )
    } yield {
      Ok(views.html.tokens.index(uiData(request), PaginatedCollection(page, tokens)))
    }
  }

  def show(id: String) = User.async { implicit request =>
    withToken(request, id) { token =>
      Future {
        Ok(views.html.tokens.show(uiData(request), token))
      }
    }
  }

  def create() = User { implicit request =>
    Ok(views.html.tokens.create(uiData(request), TokensController.tokenForm))
  }

  def postCreate = User.async { implicit request =>
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

  def postDelete(id: String) = User.async { implicit request =>
    deltaClient(request).tokens.deleteById(id).map { _ =>
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
