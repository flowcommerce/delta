package controllers

import io.flow.delta.v0.errors.UnitResponse
import io.flow.delta.v0.models.{Membership, MembershipForm, Role}
import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.controllers.{FlowControllerComponents, IdentifiedRequest}
import io.flow.play.util.{Config, PaginatedCollection, Pagination}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class MembersController @javax.inject.Inject() (
  val config: Config,
  deltaClientProvider: DeltaClientProvider,
  controllerComponents: ControllerComponents,
  flowControllerComponents: FlowControllerComponents
)(implicit ec: ExecutionContext)
  extends controllers.BaseController(deltaClientProvider, controllerComponents, flowControllerComponents) {

  override def section = None

  def index(orgId: String, page: Int = 0) = User.async { implicit request =>
    withOrganization(request, orgId) { org =>
      for {
        memberships <- deltaClient(request).memberships.get(
          organization = Some(org.id),
          limit = Pagination.DefaultLimit.toLong + 1L,
          offset = page * Pagination.DefaultLimit.toLong
        )
      } yield {
        Ok(
          views.html.members.index(
            uiData(request).copy(organization = Some(org.id)),
            org,
            PaginatedCollection(page, memberships)
          )
      )
      }
    }
  }

  def create(orgId: String) = User.async { implicit request =>
    withOrganization(request, orgId) { org =>
      Future {
        Ok(
          views.html.members.create(
            uiData(request).copy(organization = Some(org.id)),
            org,
            MembersController.uiForm
          )
        )
      }
    }
  }

  def postCreate(orgId: String) = User.async { implicit request =>
    withOrganization(request, orgId) { org =>
      val boundForm = MembersController.uiForm.bindFromRequest

      organizations(request).flatMap { _ =>
        boundForm.fold (

          formWithErrors => Future {
            Ok(views.html.members.create(uiData(request).copy(organization = Some(org.id)), org, formWithErrors))
          },

          uiForm => {
            deltaClient(request).users.get(email = Some(uiForm.email)).flatMap { users =>
              users.headOption match {
                case None => Future {
                  Ok(views.html.members.create(uiData(request).copy(
                    organization = Some(org.id)), org, boundForm, Seq("User with specified email not found"))
                  )
                }
                case Some(user) => {
                  deltaClient(request).memberships.post(
                    MembershipForm(
                      organization = org.id,
                      userId = user.id,
                      role = Role(uiForm.role)
                    )
                  ).map { membership =>
                    Redirect(routes.MembersController.index(org.id)).flashing("success" -> s"User added as ${membership.role}")
                  }.recover {
                    case response: io.flow.delta.v0.errors.GenericErrorResponse => {
                      Ok(views.html.members.create(
                        uiData(request).copy(organization = Some(org.id)), org, boundForm, response.genericError.messages)
                      )
                    }
                  }
                }
              }
            }
          }
        )
      }
    }
  }

  def postDelete(orgId: String, id: String) = User.async { implicit request =>
    withOrganization(request, orgId) { org =>
      deltaClient(request).memberships.deleteById(id).map { _ =>
        Redirect(routes.MembersController.index(org.id)).flashing("success" -> s"Membership deleted")
      }.recover {
        case UnitResponse(404) => {
          Redirect(routes.MembersController.index(org.id)).flashing("warning" -> s"Membership not found")
        }
      }
    }
  }

  def postMakeMember(orgId: String, id: String) = User.async { implicit request =>
    makeRole(request, orgId, id, Role.Member)
  }

  def postMakeAdmin(orgId: String, id: String) = User.async { implicit request =>
    makeRole(request, orgId, id, Role.Admin)
  }

  def makeRole[T](
    request: IdentifiedRequest[T],
    orgId: String,
    id: String,
    role: Role
  ): Future[Result] = {
    withOrganization(request, orgId) { org =>
      withMembership(org.id, request, id) { membership =>
        deltaClient(request).memberships.post(
          MembershipForm(
            organization = membership.organization.id,
            userId = membership.user.id,
            role = role
          )
        ).map { membership =>
          Redirect(routes.MembersController.index(membership.organization.id)).flashing("success" -> s"User added as ${membership.role}")
        }.recover {
          case response: io.flow.delta.v0.errors.GenericErrorResponse => {
            Redirect(routes.MembersController.index(membership.organization.id)).flashing("warning" -> response.genericError.messages.mkString(", "))
          }
        }
      }
    }
  }

  def withMembership[T](
    org: String,
    request: IdentifiedRequest[T],
    id: String
  )(
    f: Membership => Future[Result]
  ) = {
    deltaClient(request).memberships.getById(id).flatMap { membership =>
      f(membership)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.MembersController.index(org)).flashing("warning" -> s"Membership not found")
      }
    }
  }
}

object MembersController {

  case class UiForm(
    role: String,
    email: String
  )

  private val uiForm = Form(
    mapping(
      "role" -> nonEmptyText,
      "email" -> nonEmptyText
    )(UiForm.apply)(UiForm.unapply)
  )

}
