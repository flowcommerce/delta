package io.flow.delta.actors.functions

import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.api.lib.GithubUtil
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.v0.models.Project
import io.flow.postgresql.Authorization
import db.{ShasDao, ShasWriteDao, UsersDao}
import scala.concurrent.Future

object SyncShas extends ProjectSupervisorFunction {

  override val stage = ProjectStage.SyncShas

  override def run(
    project: Project,
    config: ConfigProject
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    Future.sequence {
      config.branches.map { branch =>
        SyncShas(project, branch.name).run
      }
    }.map {
      SupervisorResult.merge(_)
    }
  }

}

/**
  * Look up the sha for the master branch from github, and record it
  * in the shas table.
  */
case class SyncShas(project: Project, branchName: String) extends Github {

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
          val existing = ShasDao.findByProjectIdAndBranch(Authorization.All, project.id, branchName).map(_.hash)

          client.refs.getByRef(repo.owner, repo.project, s"heads/$branchName").map { branch =>
            val branchSha = branch.`object`.sha
            existing == Some(branchSha) match {
              case true => {
                SupervisorResult.Ready(s"Shas table already records that branch[$branchName] is at $branchSha")
              }
              case false => {
                shasWriteDao.upsertBranch(UsersDao.systemUser, project.id, branchName, branchSha)
                SupervisorResult.Change(s"Updated branch[$branchName] sha to $branchSha")
              }
            }
          }
        }
      }
    }
  }

}
