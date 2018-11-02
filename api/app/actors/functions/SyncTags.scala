package io.flow.delta.actors.functions

import javax.inject.Inject

import db.{TagsDao, TagsWriteDao}
import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.api.lib.{GithubUtil, Repo}
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.v0.models.Project
import io.flow.util.Constants
import io.flow.postgresql.Authorization
import play.api.Application

import scala.concurrent.Future

object SyncTags extends ProjectSupervisorFunction {

  override val stage = ProjectStage.SyncTags

  override def run(
    project: Project,
    config: ConfigProject
  ) (
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult] = {
    val syncTags = app.injector.instanceOf[SyncTags]
    syncTags.run(project)
  }

}

/**
  * Downloads all tags from github and stores in local DB
  */
class SyncTags @Inject()(
  github: Github,
  tagsDao: TagsDao,
  tagsWriteDao: TagsWriteDao
) {

  val InitialTag = "0.0.1"

  private[this] def projectRepo(project: Project): Repo = GithubUtil.parseUri(project.uri).right.getOrElse {
    sys.error(s"Project id[${project.id}] uri[${project.uri}]: Could not parse")
  }

  def run(project: Project)(
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    val repo: Repo = projectRepo(project)

    github.withGithubClient(project.user.id) { client =>
      client.tags.getTags(repo.owner, repo.project).map { tags =>
        val localTags = GithubUtil.toTags(tags)
        // latest tag version first to set the expected state to
        // that version, if needed. Otherwise we will trigger a
        // state update for every tag.
        localTags.reverse.flatMap { tag =>
          tagsDao.findByProjectIdAndName(Authorization.All, project.id, tag.semver.label) match {
            case None => {
              tagsWriteDao.upsert(Constants.SystemUser, project.id, tag.semver.label, tag.sha)
              Some(tag.semver.label)
            }

            case Some(_) => {
              None
            }
          }
        }.toList match {
          case Nil => SupervisorResult.Ready(s"No new tags found")
          case tag :: Nil => SupervisorResult.Change(s"One new tag found: $tag")
          case multiple => SupervisorResult.Change(s"New tags found: ${multiple.mkString(", ")}")
        }
      }
    }
  }

}
