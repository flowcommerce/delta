package io.flow.delta.actors.functions

import javax.inject.Inject
import db.{BuildDesiredStatesDao, ConfigsDao, TagsDao}
import io.flow.delta.actors.{BuildSupervisorFunction, SupervisorResult}
import io.flow.delta.config.v0.models.{BuildConfig, BuildStage, ConfigError, ConfigProject, ConfigUndefinedType, EcsBuildConfig, K8sBuildConfig}
import io.flow.delta.lib.StateFormatter
import io.flow.delta.v0.models.{Build, StateForm, Version}
import io.flow.util.Constants
import io.flow.postgresql.{Authorization, OrderBy}
import k8s.KubernetesService
import lib.BuildConfigUtil
import play.api.Application

import scala.concurrent.Future

object SetDesiredState extends BuildSupervisorFunction {

  override val stage = BuildStage.SetDesiredState

  override def run(
    build: Build,
    cfg: BuildConfig,
  ) (
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult] = Future {
    val setDesiredState = app.injector.instanceOf[SetDesiredState]
    setDesiredState.run(build)
  }

}

/**
  * For builds that have auto deploy turned on, we set the desired
  * state to 100% of traffic on the latest tag.
  */
class SetDesiredState @Inject()(
  buildDesiredStatesDao: BuildDesiredStatesDao,
  kubernetesService: KubernetesService,
  configsDao: ConfigsDao,
  tagsDao: TagsDao,
) {

  val DefaultNumberInstances = 2L

  def run(build: Build): SupervisorResult = {
    tagsDao.findAll(
      Authorization.All,
      projectId = Some(build.project.id),
      orderBy = OrderBy("-tags.sort_key"),
      limit = Some(1)
    ).headOption match {
      case None => {
        SupervisorResult.Checkpoint("Project does not have any tags")
      }

      case Some(latestTag) => {
        buildDesiredStatesDao.findByBuildId(Authorization.All, build.id) match {
          case None => {
            setVersions(Seq(Version(latestTag.name, instances = numberInstances(build, latestTag.name))), build)
          }

          case Some(state) => {
            val targetVersions = Seq(Version(latestTag.name, instances = numberInstances(build, latestTag.name)))

            if (state.versions == targetVersions) {
              SupervisorResult.Ready("Desired versions remain: " + targetVersions.map(_.name).mkString(", "))
            } else {
              setVersions(targetVersions, build)
            }
          }
        }
      }
    }
  }

  def setVersions(versions: Seq[Version], build: Build): SupervisorResult = {
    assert(versions.nonEmpty, "Must have at least one version")

    buildDesiredStatesDao.upsert(
      Constants.SystemUser,
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
  private def numberInstances(build: Build, version: String): Long = {
    configsDao.findByProjectId(Authorization.All, build.project.id).map(_.config) match {
      case None => DefaultNumberInstances
      case Some(config) => {
        config match {
          case cfg: ConfigProject => {
            BuildConfigUtil.findBuildByName(cfg.builds, build.name) match {
              case Some(_: K8sBuildConfig) => kubernetesService.getDesiredReplicaNumber(build.project.id, version).getOrElse(DefaultNumberInstances)
              case Some(ecsConfig: EcsBuildConfig) => ecsConfig.initialNumberInstances
              case _ => DefaultNumberInstances
            }
          }
          case ConfigError(_) | ConfigUndefinedType(_) => DefaultNumberInstances
        }
      }
    }
  }
}
