package io.flow.delta.actors.functions

import db.{TagsDao, TagsWriteDao, UsersDao}
import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.api.lib.GithubUtil
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.v0.models.Project
import io.flow.postgresql.Authorization
import play.api.Logger
import scala.concurrent.Future

/**
  * Downloads all tags from github and stores in local DB
  */
object SyncTags extends ProjectSupervisorFunction {

  val InitialTag = "0.0.1"

  override val stage = ProjectStage.SyncTags

  override def run(
    project: Project,
    config: ConfigProject
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    SyncTags(project).run
  }

}

case class SyncTags(project: Project) extends Github {

  private[this] lazy val tagsWriteDao = play.api.Play.current.injector.instanceOf[TagsWriteDao]

  private[this] val repo = GithubUtil.parseUri(project.uri).right.getOrElse {
    sys.error(s"Project id[${project.id}] uri[${project.uri}]: Could not parse")
  }

  def run(
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    withGithubClient(project.user.id) { client =>
      client.tags.getTags(repo.owner, repo.project).map { tags =>
        val localTags = GithubUtil.toTags(tags)
        // latest tag version first to set the expected state to
        // that version, if needed. Otherwise we will trigger a
        // state update for every tag.
        localTags.reverse.flatMap { tag =>
          TagsDao.findByProjectIdAndName(Authorization.All, project.id, tag.semver.label) match {
            case None => {
              tagsWriteDao.upsert(UsersDao.systemUser, project.id, tag.semver.label, tag.sha)
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
