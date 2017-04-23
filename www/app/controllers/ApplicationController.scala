package controllers

import io.flow.delta.v0.models.Version
import io.flow.delta.v0.errors.UnitResponse
import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.util.{Pagination, PaginatedCollection}
import org.joda.time.DateTime
import play.api.i18n.MessagesApi
import play.api.mvc._

/**
  * Wrapper to simplify display
  */
case class BuildView(val dashboardBuild: io.flow.delta.v0.models.DashboardBuild) {

  private[this] val MinutesUntilError = 30

  private[this] val lastNames = format(dashboardBuild.last.versions)
  private[this] val desiredNames = format(dashboardBuild.desired.versions)

  private[this] val lastInstances = formatInstances(dashboardBuild.last.versions)
  private[this] val desiredInstances = formatInstances(dashboardBuild.desired.versions)

  val status: Option[String] = {
    lastNames == desiredNames match {
      case true =>
        if(lastInstances != desiredInstances)
          Some(
            if (dashboardBuild.desired.timestamp.isBefore(new DateTime().minusMinutes(MinutesUntilError))) {
              "danger"
            } else {
              "warning"
            }
          )
        else
          None
      case false => {
        Some(
          if (dashboardBuild.desired.timestamp.isBefore(new DateTime().minusMinutes(MinutesUntilError))) {
            "danger"
          } else {
            "warning"
          }
        )
      }
    }
  }

  val label = {
    lastNames == desiredNames match {
      case true => {
        if(lastInstances != desiredInstances)
          s"Transitioning from $lastNames to $desiredNames"
        else
          s"Running $desiredNames"
      }
      case false => {
        s"Transitioning from $lastNames to $desiredNames"
      }
    }

  }

  def format(versions: Seq[Version]): String = {
    versions.map(_.name) match {
      case Nil => "Nothing"
      case names => names.mkString(", ")
    }
  }

  def formatInstances(versions: Seq[Version]): String = {
    versions.map(_.instances) match {
      case Nil => "Nothing"
      case instances => instances.mkString(", ")
    }
  }

}

class ApplicationController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val deltaClientProvider: DeltaClientProvider
) extends BaseController(tokenClient, deltaClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global
 
  override def section = Some(io.flow.delta.www.lib.Section.Dashboard)

  def redirect = Action { request =>
    Redirect(request.path + "/")
  }

  def index(organization: Option[String], buildsPage: Int = 0) = Identified.async { implicit request =>
    for {
      dashboardBuilds <- deltaClient(request).dashboardBuilds.get(
        limit = Pagination.DefaultLimit+1,
        offset = buildsPage * Pagination.DefaultLimit
      ).recover {
        case UnitResponse(401) => Nil
      }
    } yield {
      Ok(
        views.html.index(
          uiData(request).copy(organization = organization),
          PaginatedCollection(buildsPage, dashboardBuilds.map(BuildView(_)))
        )
      )
    }
  }

}
