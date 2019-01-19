package io.flow.delta.lib

import io.flow.delta.v0.models.{Build, DashboardBuild, Docker}

object BuildNames {

  val DefaultBuildName = "root"

  /**
    * Given a build, returns the full docker image name
    * (e.g. flowcommerce/delta-api)
    */
  def dockerImageName(docker: Docker, build: Build): String = {
    docker.organization + "/" + projectName(build)
  }

  def dockerImageName(docker: Docker, build: Build, version: String): String = {
    dockerImageName(docker, build) + s":$version"
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
