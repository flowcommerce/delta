package io.flow.delta.actors

import db.{ConfigsDao, OrganizationsDao, ProjectsDao}
import io.flow.delta.api.lib.{GithubUtil, Repo}
import io.flow.delta.config.v0.models.{ConfigError, ConfigProject, ConfigUndefinedType}
import io.flow.delta.v0.models.{Organization, Project}
import io.flow.postgresql.Authorization
import play.api.Logger

trait DataProject {

  def configsDao: ConfigsDao
  def organizationsDao: OrganizationsDao
  def projectsDao: ProjectsDao

  private[this] var dataProject: Option[Project] = None

  /**
    * Looks up the project with the specified ID, setting the local
    * dataProject var to that project
    */
  def setProjectId(id: String) {
    dataProject = projectsDao.findById(Authorization.All, id)
    if (dataProject.isEmpty) {
      Logger.warn(s"Could not find project with id[$id]")
    }
  }

  /**
    * Invokes the specified function w/ the current project, but only
    * if we have a project set.
    */
  def withProject[T](f: Project => T): Option[T] = {
    dataProject.map { f(_) }
  }

  /**
    * Invokes the specified function w/ the current organization, but only
    * if we have one
    */
  def withOrganization[T](f: Organization => T): Option[T] = {
    dataProject.flatMap { project =>
      organizationsDao.findById(Authorization.All, project.organization.id).map { org =>
        f(org)
      }
    }
  }

  /**
    * Invokes the specified function w/ the current project config, if
    * it is valid.
    */
  def withConfig[T](f: ConfigProject => T): Option[T] = {
    dataProject.flatMap { project =>
      configsDao.findByProjectId(Authorization.All, project.id).map(_.config) match {
        case None => {
          Logger.info(s"Project[${project.id}] does not have a configuration")
          None
        }

        case Some(config) => config match {
          case c: ConfigProject => {
            Some(f(c))
          }
          case ConfigError(_) | ConfigUndefinedType(_) => {
            Logger.info(s"Project[${project.id}] has an erroneous configuration")
            None
          }
        }
      }
    }
  }

  /**
    * Invokes the specified function w/ the current project, parsed
    * into a Repo, but only if we have a project set and it parses
    * into a valid Repo.
    */
  def withRepo[T](f: Repo => T): Option[T] = {
    dataProject.flatMap { project =>
      GithubUtil.parseUri(project.uri) match {
        case Left(error) => {
          Logger.warn(s"Cannot parse repo from project id[${project.id}] uri[${project.uri}]: $error")
          None
        }
        case Right(repo) => {
          Some(
            f(repo)
          )
        }
      }
    }
  }

}