package controllers

import javax.inject.Inject

import db.{OrganizationsDao, ProjectsDao, UsersDao}
import io.flow.common.v0.models.{User, UserReference}
import io.flow.delta.v0.models.{Organization, Project}
import io.flow.error.v0.models.json._
import io.flow.play.util.Validation
import io.flow.postgresql.{Authorization, OrderBy}
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}

class Helpers @Inject()(
  organizationsDao: OrganizationsDao,
  projectsDao: ProjectsDao,
  usersDao: UsersDao
) {

  def withOrderBy(sort: String)(
    f: OrderBy => Result
  ) = {
    OrderBy.parse(sort) match {
      case Left(errors) => {
        Results.UnprocessableEntity(Json.toJson(Validation.invalidSort(errors)))
      }
      case Right(orderBy) => {
        f(orderBy)
      }
    }
  }
  
  def withOrganization(user: UserReference, id: String)(
    f: Organization => Result
  ) = {
    organizationsDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(organization) => {
        f(organization)
      }
    }
  }

  def withProject(user: UserReference, id: String)(
    f: Project => Result
  ): Result = {
    projectsDao.findById(Authorization.User(user.id), id) match {
      case None => {
        Results.NotFound
      }
      case Some(project) => {
        f(project)
      }
    }
  }

  def withUser(id: String)(
    f: User => Result
  ) = {
    usersDao.findById(id) match {
      case None => {
        Results.NotFound
      }
      case Some(user) => {
        f(user)
      }
    }
  }

}
