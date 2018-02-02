package controllers

import db.{OrganizationsDao, ProjectsDao}
import io.flow.common.v0.models.json._
import io.flow.delta.config.v0.models.json._
import io.flow.delta.api.lib.Github
import io.flow.delta.lib.config.{Defaults, Parser}
import io.flow.github.v0.models.json._
import io.flow.play.util.{Config, Validation}
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future

@javax.inject.Singleton
class Repositories @javax.inject.Inject() (
  override val config: Config,
  override val tokenClient: io.flow.token.v0.interfaces.Client,
  val github: Github,
  parser: Parser
) extends Controller with BaseIdentifiedRestController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def get(
    owner: Option[String] = None, // Ex: flowcommerce
    name: Option[String] = None,  // Ex: user
    organizationId: Option[String] = None,
    existingProject: Option[Boolean] = None,
    limit: Long = 25,
    offset: Long = 0
  ) = Identified.async { request =>
    if (!existingProject.isEmpty && organizationId.isEmpty) {
      Future {
        UnprocessableEntity(Json.toJson(Validation.error("When filtering by existing projects, you must also provide the organization_id")))
      }

    } else {
      val auth = authorization(request)
      val org = organizationId.flatMap { OrganizationsDao.findById(auth, _)}

      // Set limit to 1 if we are guaranteed at most 1 record back
      val actualLimit = if (offset == 0 && !name.isEmpty && !owner.isEmpty) { 1 } else { limit }

      github.repositories(request.user, offset, actualLimit) { r =>
        (name match {
          case None => true
          case Some(n) => n.toLowerCase == r.name.toLowerCase
        }) &&
        (owner match {
          case None => true
          case Some(o) => o.toLowerCase == r.owner.login.toLowerCase
        }) &&
        (org match {
          case None => true
          case Some(org) => {
            existingProject.isEmpty ||
            existingProject == Some(true) && !ProjectsDao.findByOrganizationIdAndName(auth, org.id, r.name).isEmpty ||
            existingProject == Some(false) && ProjectsDao.findByOrganizationIdAndName(auth, org.id, r.name).isEmpty
          }
        })
      }.map { results =>
        Ok(Json.toJson(results))
      }
    }
  }

  /**
    * Fetches the delta configuration for this github repo, using the
    * .delta file if available or the default for delta.
    */
  def getConfigByOwnerAndRepo(
    owner: String,
    repo: String
  ) = Identified.async { request =>
    github.dotDeltaFile(request.user, owner, repo).map { result =>
      Ok(
        Json.toJson(
          result.map { parser.parse(_) }.getOrElse { Defaults.Config }
        )
      )
    }
  }

}
