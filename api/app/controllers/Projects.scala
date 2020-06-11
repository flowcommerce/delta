package controllers

import com.github.ghik.silencer.silent
import db._
import io.flow.common.v0.models.UserReference
import io.flow.delta.actors.MainActor
import io.flow.delta.v0.models.json._
import io.flow.delta.v0.models.{Build, BuildState, ProjectForm}
import io.flow.error.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import io.flow.play.util.Validation
import io.flow.postgresql.Authorization
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class Projects @javax.inject.Inject() (
                                        @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
                                        buildsDao: BuildsDao,
                                        buildDesiredStatesDao: BuildDesiredStatesDao,
                                        buildLastStatesDao: InternalBuildLastStatesDao,
                                        helpers: Helpers,
                                        imagesDao: ImagesDao,
                                        projectsDao: ProjectsDao,
                                        projectsWriteDao: ProjectsWriteDao,
                                        membershipsDao: MembershipsDao,
                                        val controllerComponents: ControllerComponents,
                                        val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    organization: Option[String],
    name: Option[String],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified { request =>
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          projectsDao.findAll(
            authorization(request),
            ids = optionals(id),
            name = name,
            organizationId = organization,
            limit = Some(limit),
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified { request =>
    helpers.withProject(request.user, id) { project =>
      Ok(Json.toJson(project))
    }
  }

  def post() = Identified { request =>
    JsValue.sync(request.contentType, request.body) { js =>
      js.validate[ProjectForm] match {
        case e: JsError => {
          UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
        }
        case s: JsSuccess[ProjectForm] => {
          println(s.get)
          projectsWriteDao.create(request.user, s.get) match {
            case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
            case Right(project) => Created(Json.toJson(project))
          }
        }
      }
    }
  }

  def putById(id: String) = Identified { request =>
    helpers.withProject(request.user, id) { project =>
      JsValue.sync(request.contentType, request.body) { js =>
        js.validate[ProjectForm] match {
          case e: JsError => {
            UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
          }
          case s: JsSuccess[ProjectForm] => {
            projectsWriteDao.update(request.user, project, s.get) match {
              case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
              case Right(updated) => Ok(Json.toJson(updated))
            }
          }
        }
      }
    }
  }

  def deleteById(id: String) = Identified { request =>
    helpers.withProject(request.user, id) { project =>
      // The user must belong to the organization which owns the project.
      // This prevent everyone from being able to delete public projects.
      membershipsDao.findByOrganizationIdAndUserId(authorization(request), project.organization.id, request.user.id) match {
        case None =>
          Unauthorized
        case Some(_) =>
          projectsWriteDao.delete(request.user, project)
          NoContent
      }
    }
  }
 
  def getBuildsAndStatesById(id: String) = Identified { request =>
    helpers.withProject(request.user, id) { project =>
      Ok(
        Json.toJson(
          buildsDao.findAllByProjectId(authorization(request), project.id).map { build =>
            BuildState(
              name = build.name,
              desired = buildDesiredStatesDao.findByBuildId(authorization(request), build.id),
              last = buildLastStatesDao.findByBuildId(authorization(request), build.id),
              latestImage = imagesDao.findAll(buildId = Some(build.id), limit = Some(1)).headOption.map( i => s"${i.name}:${i.version}" )
            )
          }
        )
      )
    }
  }

  def postEventsAndPursueDesiredStateById(id: String) = Identified { request =>
    helpers.withProject(request.user, id) { project =>
      mainActor ! MainActor.Messages.ProjectSync(project.id)
      NoContent
    }
  }

  @silent def getBuildsAndStatesAndDesiredByIdAndBuildName(id: String, buildName: String) = TODO

  @silent def postBuildsAndStatesAndDesiredByIdAndBuildName(id: String, buildName: String) = TODO

  @silent def getBuildsAndStatesAndLastByIdAndBuildName(id: String, buildName: String) = TODO

  def withBuild(user: UserReference, projectId: String, name: String)(
    f: Build => Result
  ): Result = {
    buildsDao.findByProjectIdAndName(Authorization.User(user.id), projectId, name) match {
      case None => {
        Results.NotFound
      }
      case Some(build) => {
        f(build)
      }
    }
  }

}
