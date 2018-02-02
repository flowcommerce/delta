package io.flow.delta.actors.functions

import db.{BuildLastStatesDao, BuildDesiredStatesDao, BuildDesiredStatesWriteDao, ConfigsDao, TagsDao, UsersDao}
import io.flow.delta.actors.{BuildSupervisorFunction, SupervisorResult}
import io.flow.delta.config.v0.models.{BuildStage, ConfigError, ConfigProject, ConfigUndefinedType}
import io.flow.delta.lib.StateFormatter
import io.flow.delta.v0.models.{Build, State, StateForm, Version}
import io.flow.postgresql.{Authorization, OrderBy}
import scala.concurrent.Future

/**
  * For builds that have auto deploy turned on, we set the desired
  * state to 100% of traffic on the latest tag.
  */
object SetDesiredState extends BuildSupervisorFunction {

  val DefaultNumberInstances = 2

  override val stage = BuildStage.SetDesiredState

  override def run(
    build: Build
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    Future {
      SetDesiredState(build).run
    }
  }

}

case class SetDesiredState(build: Build) extends Github {

  private[this] val buildDesiredStatesWriteDao = play.api.Play.current.injector.instanceOf[BuildDesiredStatesWriteDao]
  private[this] val configsDao = play.api.Play.current.injector.instanceOf[ConfigsDao]

  def run(): SupervisorResult = {
    TagsDao.findAll(
      Authorization.All,
      projectId = Some(build.project.id),
      orderBy = OrderBy("-tags.sort_key"),
      limit = 1
    ).headOption match {
      case None => {
        SupervisorResult.Checkpoint("Project does not have any tags")
      }

      case Some(latestTag) => {
        BuildDesiredStatesDao.findByBuildId(Authorization.All, build.id) match {
          case None => {
            setVersions(Seq(Version(latestTag.name, instances = numberInstances(build))))
          }

          case Some(state) => {
            val targetVersions = Seq(Version(latestTag.name, instances = numberInstances(build)))

            if (state.versions == targetVersions) {
              SupervisorResult.Ready("Desired versions remain: " + targetVersions.map(_.name).mkString(", "))
            } else {
              setVersions(targetVersions)
            }
          }
        }
      }
    }
  }

  def setVersions(versions: Seq[Version]): SupervisorResult = {
    assert(!versions.isEmpty, "Must have at least one version")

    buildDesiredStatesWriteDao.upsert(
      UsersDao.systemUser,
      build,
      StateForm(
        versions = versions
      )
    )

    SupervisorResult.Change("Desired state changed to: " + StateFormatter.label(versions))
  }

  /**
    * By default, we create the same number of instances of the new
    * version as the total number of instances in the last state.
    */
  def numberInstances(build: Build): Long = {
    configsDao.findByProjectId(Authorization.All, build.project.id).map(_.config) match {
      case None => SetDesiredState.DefaultNumberInstances
      case Some(config) => {
        config match {
          case cfg: ConfigProject => {
            cfg.builds.find(_.name == build.name) match {
              case None => SetDesiredState.DefaultNumberInstances
              case Some(bc) => bc.initialNumberInstances
            }
          }
          case ConfigError(_) | ConfigUndefinedType(_) => SetDesiredState.DefaultNumberInstances
        }
      }
    }
  }
}
