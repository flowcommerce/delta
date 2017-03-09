package controllers

import io.flow.delta.v0.errors.UnitResponse
import io.flow.delta.v0.models.{EventType, Project, ProjectForm, Scms, Visibility}
import io.flow.delta.v0.models.json._
import io.flow.delta.config.v0.models.{Config, ConfigError, ConfigProject, ConfigUndefinedType}
import io.flow.delta.www.lib.DeltaClientProvider
import io.flow.play.util.{Pagination, PaginatedCollection}
import play.api.libs.json.Json
import scala.concurrent.Future
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

class ProjectsController @javax.inject.Inject() (
  val messagesApi: MessagesApi,
  override val deltaClientProvider: DeltaClientProvider
) extends BaseController(tokenClient, deltaClientProvider) {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def section = Some(io.flow.delta.www.lib.Section.Projects)

  def index(page: Int = 0) = Identified.async { implicit request =>
    for {
      projects <- deltaClient(request).projects.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(
        views.html.projects.index(
          uiData(request),
          PaginatedCollection(page, projects)
        )
      )
    }
  }

  def show(id: String) = Identified.async { implicit request =>
    withProject(request, id) { project =>
      for {
        buildStates <- deltaClient(request).projects.getBuildsAndStatesById(id)
        changeEvents <- deltaClient(request).events.get(
          projectId = Some(project.id),
          `type` = Some(EventType.Change),
          limit = 5
        )
        recentEvents <- deltaClient(request).events.get(
          projectId = Some(project.id),
          numberMinutesSinceCreation = Some(10),
          limit = 5
        )
        recentErrorEvents <- deltaClient(request).events.get(
          projectId = Some(project.id),
          numberMinutesSinceCreation = Some(60),
          hasError = Some(true),
          limit = 5
        )
        tags <- deltaClient(request).tags.get(
          projectId = Some(id),
          sort = "-tags.sort_key",
          limit = 1
        )
        shas <- deltaClient(request).shas.get(
          projectId = Some(id),
          branch = Some("master"),
          limit = 1
        )
      } yield {
        Ok(
          views.html.projects.show(
            uiData(request),
            project,
            buildStates,
            shas.headOption.map(_.hash),
            tags.headOption,
            changeEvents,
            recentEvents,
            recentErrorEvents
          )
        )
      }
    }
  }

  def github() = Identified.async { implicit request =>
    for {
      orgs <- organizations(request)
    } yield {
      orgs match {
        case Nil => {
          Redirect(routes.OrganizationsController.create(return_url = Some(request.path))).
            flashing("warning" -> "Add a new org before adding a project")
        }
        case one :: Nil => {
          Redirect(routes.ProjectsController.githubOrg(one.id))
        }
        case multiple => {
          Ok(
            views.html.projects.github(
              uiData(request), multiple
            )
          )
        }
      }
    }
  }

  def githubOrg(orgId: String, repositoriesPage: Int = 0) = Identified.async { implicit request =>
    withOrganization(request, orgId) { org =>
      for {
        repositories <- deltaClient(request).repositories.get(
          organizationId = Some(org.id),
          existingProject = Some(false),
          limit = Pagination.DefaultLimit+1,
          offset = repositoriesPage * Pagination.DefaultLimit
        )
      } yield {
        Ok(
          views.html.projects.githubOrg(
            uiData(request), org, PaginatedCollection(repositoriesPage, repositories)
          )
        )
      }
    }
  }

  def postGithubOrg(
    orgId: String,
    owner: String, // github owner, ex. flowcommerce
    name: String,  // github repo name, ex. user
    repositoriesPage: Int = 0
  ) = Identified.async { implicit request =>
    withOrganization(request, orgId) { org =>
      deltaClient(request).repositories.get(
        organizationId = Some(org.id),
        owner = Some(owner),
        name = Some(name),
        limit = 1
      ).flatMap { selected =>
        selected.headOption match {
          case None => Future {
            Redirect(routes.ProjectsController.githubOrg(orgId)).flashing("warning" -> "Project not found")
          }
          case Some(repo) => {
            deltaClient(request).repositories.getConfigByOwnerAndRepo(owner, name).flatMap { config =>
              config match {
                case ConfigError(errors) => Future {
                  Redirect(routes.ProjectsController.githubOrg(orgId)).flashing("warning" -> s"Error parsing .delta config file: ${errors.mkString(", ")}")
                }

                case ConfigUndefinedType(other) => Future {
                  Logger.warn(s"$owner/$repo returned ConfigUndefinedType[$other]")
                  Redirect(routes.ProjectsController.githubOrg(orgId)).flashing("warning" -> s"Unknown error parsing .delta config file")
                }

                case c: ConfigProject => {
                  deltaClient(request).projects.post(
                    ProjectForm(
                      organization = org.id,
                      name = repo.name,
                      scms = Scms.Github,
                      visibility = if (repo.`private`) { Visibility.Private } else { Visibility.Public },
                      uri = repo.htmlUrl,
                      config = Some(c)
                    )
                  ).map { project =>
                    Redirect(routes.ProjectsController.show(project.id)).flashing("success" -> "Project added")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def create() = Identified.async { implicit request =>
    organizations(request).map { orgs =>
      Ok(
        views.html.projects.create(
          uiData(request),
          ProjectsController.uiForm,
          orgs
        )
      )
    }
  }

  def postCreate() = Identified.async { implicit request =>
    val boundForm = ProjectsController.uiForm.bindFromRequest

    organizations(request).flatMap { orgs =>
      boundForm.fold (

        formWithErrors => Future {
          Ok(views.html.projects.create(uiData(request), formWithErrors, orgs))
        },

        uiForm => {
          deltaClient(request).projects.post(
            projectForm = ProjectForm(
              organization = uiForm.organization,
              name = uiForm.name,
              scms = Scms(uiForm.scms),
              visibility = Visibility(uiForm.visibility),
              uri = uiForm.uri,
              config = None
            )
          ).map { project =>
            Redirect(routes.ProjectsController.show(project.id)).flashing("success" -> "Project created")
          }.recover {
            case response: io.flow.delta.v0.errors.ErrorsResponse => {
              Ok(views.html.projects.create(uiData(request), boundForm, orgs, response.errors.map(_.message)))
            }
          }
        }
      )
    }
  }

  def edit(id: String) = Identified.async { implicit request =>
    withProject(request, id) { project =>
      organizations(request).map { orgs =>
        Ok(
          views.html.projects.edit(
            uiData(request),
            project,
            ProjectsController.uiForm.fill(
              ProjectsController.UiForm(
                organization = project.organization.id,
                name = project.name,
                scms = project.scms.toString,
                visibility = project.visibility.toString,
                uri = project.uri
              )
            ),
            orgs
          )
        )
      }
    }
  }

  def postEdit(id: String) = Identified.async { implicit request =>
    organizations(request).flatMap { orgs =>
      withProject(request, id) { project =>
        val boundForm = ProjectsController.uiForm.bindFromRequest
          boundForm.fold (

            formWithErrors => Future {
              Ok(views.html.projects.edit(uiData(request), project, formWithErrors, orgs))
            },

            uiForm => {
              deltaClient(request).projects.putById(
                project.id,
                ProjectForm(
                  organization = project.organization.id,
                  name = uiForm.name,
                  scms = Scms(uiForm.scms),
                  visibility = Visibility(uiForm.visibility),
                  uri = uiForm.uri
                )
              ).map { project =>
                Redirect(routes.ProjectsController.show(project.id)).flashing("success" -> "Project updated")
              }.recover {
                case response: io.flow.delta.v0.errors.ErrorsResponse => {
                  Ok(views.html.projects.edit(uiData(request), project, boundForm, orgs, response.errors.map(_.message)))
                }
              }
            }
          )
      }
    }
  }

  def postDelete(id: String) = Identified.async { implicit request =>
    deltaClient(request).projects.deleteById(id).map { response =>
      Redirect(routes.ProjectsController.index()).flashing("success" -> s"Project deleted")
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.ProjectsController.index()).flashing("warning" -> s"Project not found")
      }
    }
  }

  def postSync(id: String) = Identified.async { implicit request =>
    deltaClient(request).projects.postEventsAndPursueDesiredStateById(id).map { _ =>
      Redirect(routes.ProjectsController.show(id)).flashing("success" -> s"Project sync triggered")
    }
  }

  def withProject[T](
    request: IdentifiedRequest[T],
    id: String
  )(
    f: Project => Future[Result]
  ) = {
    deltaClient(request).projects.getById(id).flatMap { project =>
      f(project)
    }.recover {
      case UnitResponse(404) => {
        Redirect(routes.ProjectsController.index()).flashing("warning" -> s"Project not found")
      }
    }
  }

}

object ProjectsController {

  case class UiForm(
    organization: String,
    name: String,
    scms: String,
    visibility: String,
    uri: String
  )

  private val uiForm = Form(
    mapping(
      "organization" -> nonEmptyText,
      "name" -> nonEmptyText,
      "scms" -> nonEmptyText,
      "visibility" -> nonEmptyText,
      "uri" -> nonEmptyText
    )(UiForm.apply)(UiForm.unapply)
  )

}
