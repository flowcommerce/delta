package controllers

import db.{OrganizationsDao, ProjectsDao, UsersDao}
import io.flow.common.v0.models.{User, UserReference}
import io.flow.common.v0.models.json._
import io.flow.delta.v0.models.{Organization, Project}
import io.flow.play.util.Validation
import io.flow.postgresql.{Authorization, OrderBy}
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}

trait Helpers {

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
    OrganizationsDao.findById(Authorization.User(user.id), id) match {
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
    ProjectsDao.findById(Authorization.User(user.id), id) match {
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
    UsersDao.findById(id) match {
      case None => {
        Results.NotFound
      }
      case Some(user) => {
        f(user)
      }
    }
  }

}
