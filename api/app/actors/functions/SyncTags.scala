package io.flow.delta.actors.functions

import javax.inject.Inject
import db.{TagsDao, TagsWriteDao}
import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.api.lib.{GithubUtil, Repo}
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.lib.HypermediaLinks
import io.flow.delta.v0.models.Project
import io.flow.github.v0.Client
import io.flow.log.RollbarLogger
import io.flow.util.Constants
import io.flow.postgresql.Authorization
import play.api.Application

import scala.concurrent.{ExecutionContext, Future}

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
  logger: RollbarLogger,
  github: Github,
  tagsDao: TagsDao,
  tagsWriteDao: TagsWriteDao
) {

  val InitialTag = "0.0.1"

  private[this] def log(project: Project): RollbarLogger = {
    logger.
      fingerprint("SyncShas").
      withKeyValue("project_id", project.id).
      withKeyValue("project_name", project.name)
  }

  private[this] def projectRepo(project: Project): Repo = GithubUtil.parseUri(project.uri).right.getOrElse {
    log(project).withKeyValue("uri", project.uri).error("Could not parse project uri")
    sys.error(s"Project id[${project.id}] uri[${project.uri}]: Could not parse")
  }

  def run(project: Project)(
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    val repo: Repo = projectRepo(project)

    github.withGithubClient(project.user.id) { client =>
      getAllTags(client, repo).map { localTags =>
        // latest tag version first to set the expected state to
        // that version, if needed. Otherwise we will trigger a
        // state update for every tag.
        localTags.sortBy(_.semver).reverse.flatMap { tag =>
          // TODO: change to a single sql query to find all tags
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
          case Nil => SupervisorResult.Ready("No new tags found")
          case tag :: Nil => SupervisorResult.Change(s"One new tag found: $tag")
          case multiple => SupervisorResult.Change(s"New tags found: ${multiple.mkString(", ")}")
        }
      }
    }
  }

  def getAllTags(client: Client, repo: Repo, page: Long = 1L)(implicit ec: ExecutionContext): Future[Seq[GithubUtil.Tag]] = {
    println(s"getAllTags project[${repo.project}] page[$page]")
    client.tags.getTags(repo.owner, repo.project, page = page, perPage = 100).flatMap { response =>
      val links = response.headers.get("link").flatMap(HypermediaLinks.parse(_).toOption).getOrElse(HypermediaLinks())
      val localTags = GithubUtil.toTags(response.body)
      if (links.next.isDefined) {
        getAllTags(client, repo, page + 1).map { t =>
          localTags ++ t
        }
      } else {
        Future.successful(localTags)
      }
    }.recoverWith {
      case ex: Throwable => {
        logger.
          withKeyValue("repo.owner", repo.owner).
          withKeyValue("repo.project", repo.project).
          error("getTags failed", ex)
        Future.failed(ex)
      }
    }
  }

}
