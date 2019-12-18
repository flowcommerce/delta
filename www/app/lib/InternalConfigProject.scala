package lib

import io.flow.delta.config.v0.models.{ConfigProject, EcsBuildConfig, K8sBuildConfig}

case class InternalConfigProject(c: ConfigProject) {
  val ecsBuildConfigs: Seq[EcsBuildConfig] = c.builds.collect {
    case e: EcsBuildConfig => e
  }
  val k8sBuildConfigs: Seq[K8sBuildConfig] = c.builds.collect {
    case e: K8sBuildConfig => e
  }
}
