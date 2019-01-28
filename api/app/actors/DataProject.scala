package io.flow.delta.actors

import java.util.concurrent.atomic.AtomicReference

import db.{ConfigsDao, OrganizationsDao, ProjectsDao}
import io.flow.delta.api.lib.{GithubUtil, Repo}
import io.flow.delta.config.v0.models.{ConfigError, ConfigProject, ConfigUndefinedType}
import io.flow.delta.v0.models.{Organization, Project}
import io.flow.delta.v0.models.json._
import io.flow.log.RollbarLogger
import io.flow.postgresql.Authorization
import play.api.libs.json.Json

trait DataProject {

  val logger: RollbarLogger

  def configsDao: ConfigsDao
  def organizationsDao: OrganizationsDao
  def projectsDao: ProjectsDao

  private[this] val projectId: AtomicReference[Option[String]] = new AtomicReference(None)

  /**
    * Looks up the project with the specified ID, setting the local
    * findProject var to that project
    */
  def setProjectId(id: String): Unit = {
    projectId.set(Some(id))
  }
  private[this] def findProject = {
    projectId.get() match {
      case None => None
      case Some(id) => {
        projectsDao.findById(Authorization.All, id) match {
          case None => {
            logger.withKeyValue("project_id", id).warn("project not found")
            None
          }
          case Some(p) => {
            Some(p)
          }
        }
      }
    }
  }

  def getProject: Option[Project] = findProject

  /**
    * Invokes the specified function w/ the current project, but only
    * if we have a project set.
    */
  def withProject[T](f: Project => T): Unit = {
    findProject.foreach(f)
  }

  /**
    * Invokes the specified function w/ the current organization, but only
    * if we have one
    */
  def withOrganization[T](f: Organization => T): Unit = {
    findProject.foreach { project =>
      organizationsDao.findById(Authorization.All, project.organization.id).foreach { org =>
        f(org)
      }
    }
  }

  /**
    * Invokes the specified function w/ the current project config, if
    * it is valid.
    */
  def withConfig[T](f: ConfigProject => T): Option[T] = {
    findProject.flatMap { project =>
      configsDao.findByProjectId(Authorization.All, project.id).map(_.config) match {
        case None => {
          logger.withKeyValue("project_id", project.id).info(s"Project does not have a configuration")
          None
        }

        case Some(config) => config match {
          case c: ConfigProject => {
            Some(f(c))
          }
          case ConfigError(_) | ConfigUndefinedType(_) => {
            logger.withKeyValue("project_id", project.id).info(s"Project has an erroneous configuration")
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
    findProject.flatMap { project =>
      GithubUtil.parseUri(project.uri) match {
        case Left(error) => {
          logger.withKeyValue("project", Json.toJson(project)).withKeyValue("error", error).warn(s"Cannot parse repo from project")
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