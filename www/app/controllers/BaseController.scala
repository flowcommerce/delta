package controllers

import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.Client
import io.flow.delta.v0.models.Organization
import io.flow.delta.www.lib.{DeltaClientProvider, Section, UiData}
import io.flow.play.controllers.IdentifiedCookie.UserKey
import io.flow.play.controllers._
import io.flow.play.util.AuthHeaders
import play.api.i18n._
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class BaseController(
  val deltaClientProvider: DeltaClientProvider,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
)(implicit val ec: ExecutionContext) extends FlowController with I18nSupport {

  protected def onUnauthorized(requestHeader: RequestHeader): Result =
    Redirect(routes.LoginController.index(return_url = Some(requestHeader.path))).flashing("warning" -> "Please login")

  private lazy val UserActionBuilder =
    new UserActionBuilder(controllerComponents.parsers.default, onUnauthorized = onUnauthorized)
  protected def User = UserActionBuilder

  def section: Option[Section]

  def withOrganization[T](
    request: IdentifiedRequest[T],
    id: String
  ) (
    f: Organization => Future[Result]
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ) = {
    deltaClient(request).organizations.get(id = Some(Seq(id)), limit = 1).flatMap { organizations =>
      organizations.headOption match {
        case None => Future {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> s"Organization not found")
        }
        case Some(org) => {
          f(org)
        }
      }
    }
  }

  def organizations[T](
    request: IdentifiedRequest[T]
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[Seq[Organization]] = {
    deltaClient(request).organizations.get(
      userId = Some(request.user.id),
      limit = 100
    )
  }

  def uiData[T](
    request: IdentifiedRequest[T]
  ) (
    implicit ec: ExecutionContext
  ): UiData = {
    val user = Await.result(
      deltaClient(request).users.get(id = Some(request.user.id)),
      Duration(1, "seconds")
    ).headOption

    UiData(
      requestPath = request.path,
      user = user,
      section = section
    )
  }

  def deltaClient[T](request: IdentifiedRequest[T]): Client =
    deltaClientProvider.newClient(user = Some(request.user), requestId = Some(request.auth.requestId))

}

class UserActionBuilder(
  val parser: BodyParser[AnyContent],
  onUnauthorized: RequestHeader => Result
)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[IdentifiedRequest, AnyContent] {

  def invokeBlock[A](request: Request[A], block: (IdentifiedRequest[A]) => Future[Result]): Future[Result] =
    request.session.get(UserKey) match {
      case None => Future.successful(onUnauthorized(request))
      case Some(userId) =>
        val auth = AuthHeaders.user(UserReference(id = userId))
        block(new IdentifiedRequest(auth, request))
    }
}
