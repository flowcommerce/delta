package io.flow.delta.lib

import io.flow.delta.config.v0.models.{Build, EcsBuildConfig}

sealed trait DockerHost
object DockerHost {
  case object DockerHub extends DockerHost
  case object Ecr extends DockerHost

  def apply(build: Build): DockerHost = {
    build match {
      case b: EcsBuildConfig => b.version.filterNot(_.startsWith("1")).fold(DockerHub: DockerHost)(_ => Ecr)
      case _ => DockerHub
    }
  }
}
