package io.flow.delta.actors.functions

import javax.inject.Inject

import db.{ShasDao, ShasWriteDao}
import io.flow.delta.actors.{ProjectSupervisorFunction, SupervisorResult}
import io.flow.delta.api.lib.GithubUtil
import io.flow.delta.config.v0.models.{ConfigProject, ProjectStage}
import io.flow.delta.v0.models.Project
import io.flow.play.util.Constants
import io.flow.postgresql.Authorization
import play.api.Application

import scala.concurrent.Future

object SyncShas extends ProjectSupervisorFunction {

  override val stage = ProjectStage.SyncShas

  override def run(
    project: Project,
    config: ConfigProject
  ) (
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult] = {
    val syncShas = app.injector.instanceOf[SyncShas]
    Future.sequence {
      config.branches.map { branch =>
        syncShas.run(project, branch.name)
      }
    }.map(SupervisorResult.merge)
  }
}

class SyncShas @Inject()(
  github: Github,
  shasDao: ShasDao,
  shasWriteDao: ShasWriteDao,
) {

  def run(project: Project, branchName: String)(
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    GithubUtil.parseUri(project.uri) match {
      case Left(error) => {
        Future {
          SupervisorResult.Error(s"Could not parse project uri[${project.uri}]")
        }
      }

      case Right(repo) => {
        github.withGithubClient(project.user.id) { client =>
          val existing = shasDao.findByProjectIdAndBranch(Authorization.All, project.id, branchName).map(_.hash)

          client.refs.getByRef(repo.owner, repo.project, s"heads/$branchName").map { branch =>
            val branchSha = branch.`object`.sha
            existing == Some(branchSha) match {
              case true => {
                SupervisorResult.Ready(s"Shas table already records that branch[$branchName] is at $branchSha")
              }
              case false => {
                shasWriteDao.upsertBranch(Constants.SystemUser, project.id, branchName, branchSha)
                SupervisorResult.Change(s"Updated branch[$branchName] sha to $branchSha")
              }
            }
          }
        }
      }
    }
  }

}
