package io.flow.delta.lib

import io.flow.delta.config.v0.models.Build

sealed trait DockerHost
object DockerHost {
  case object DockerHub extends DockerHost
  case object Ecr extends DockerHost

  def apply(build: Build): DockerHost = {
    build.version.filterNot(_.startsWith("1")).fold(DockerHub: DockerHost)(_ => Ecr)
  }
}

