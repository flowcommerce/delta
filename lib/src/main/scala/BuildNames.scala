package io.flow.delta.lib

import io.flow.delta.config.v0.models.BuildConfig
import io.flow.delta.v0.models.{Build, DashboardBuild, Docker}

object BuildNames {

  val DefaultBuildName = "root"

  /**
    * Given a build, returns the full docker image name
    * (e.g. flowcommerce/delta-api)
    */
  def dockerImageName(docker: Docker, build: Build, cfg: BuildConfig): String = {
    DockerHost(cfg) match {
      case DockerHost.Ecr => ecrDockerImageName(docker, build)
      case DockerHost.DockerHub => docker.organization + "/" + projectName(build)
    }
  }

  private[this] def ecrDockerImageName(docker: Docker, build: Build): String = {
    (docker.organization match {
      case "flowvault" => sys.error("TODO: Not yet supporting flowvault")
      case "flowcommerce" | "apicollective" => "479720515435.dkr.ecr.us-east-1.amazonaws.com" // TODO: Move somewhere
    }) + "/" + projectName(build)
  }

  def dockerImageName(docker: Docker, build: Build, cfg: BuildConfig, version: String): String = {
    dockerImageName(docker, build, cfg) + s":$version"
  }

  /**
    * Given a build, returns the project name (without organization).
    * (e.g. registry or delta-api)
    */
  def projectName(build: Build): String = {
    projectName(build.project.id, build.name)
  }

  def projectName(dashboardBuild: DashboardBuild): String = {
    projectName(dashboardBuild.project.id, dashboardBuild.name)
  }

  private[this] def projectName(projectId: String, buildName: String): String = {
    buildName match {
      case DefaultBuildName => projectId
      case _ => projectId + "-" + buildName
    }
  }

}
