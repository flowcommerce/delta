package io.flow.delta.actors.functions

import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.api.lib.GithubUtil
import io.flow.delta.v0.models.Project
import io.flow.postgresql.Authorization
import db.{ShasDao, ShasWriteDao, UsersDao}
import scala.concurrent.Future

object SyncMasterSha extends ProjectSupervisorFunction {

  def run(
    project: Project
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    SyncMasterSha(project).run
  }

}

/**
  * Look up the sha for the master branch from github, and record it
  * in the shas table.
  */
case class SyncMasterSha(project: Project) extends Github {

  private[this] lazy val shasWriteDao = play.api.Play.current.injector.instanceOf[ShasWriteDao]

  def run(
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    GithubUtil.parseUri(project.uri) match {
      case Left(error) => {
        Future {
          SupervisorResult.Error(s"Could not parse project uri[${project.uri}]")
        }
      }

      case Right(repo) => {
        withGithubClient(project.user.id) { client =>
          val existing = ShasDao.findByProjectIdAndMaster(Authorization.All, project.id).map(_.hash)

          client.refs.getByRef(repo.owner, repo.project, "heads/master").map { master =>
            val masterSha = master.`object`.sha
            existing == Some(masterSha) match {
              case true => {
                SupervisorResult.Ready(s"Shas table already records that master is at $masterSha")
              }
              case false => {
                shasWriteDao.upsertMaster(UsersDao.systemUser, project.id, masterSha)
                SupervisorResult.Change(s"Updated master sha to $masterSha")
              }
            }
          }
        }
      }
    }
  }

}
