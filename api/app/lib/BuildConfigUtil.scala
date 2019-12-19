package lib

import io.flow.delta.config.v0.models.{BuildConfig, BuildConfigUndefinedType, EcsBuildConfig, K8sBuildConfig}

object BuildConfigUtil {

  def getName(config: BuildConfig): String = {
    config match {
      case c: EcsBuildConfig => c.name
      case c: K8sBuildConfig => c.name
      case BuildConfigUndefinedType(other) => sys.error(s"Invalid config type: $other")
    }
  }

  def findBuildByName(configs: Seq[BuildConfig], name: String): Option[BuildConfig] = {
    configs.find {
      case c: EcsBuildConfig => c.name == name
      case c: K8sBuildConfig => c.name == name
      case _: BuildConfigUndefinedType => false
    }
  }

  def findEcsBuildByName(configs: Seq[BuildConfig], name: String): Option[EcsBuildConfig] = {
    findBuildByName(configs, name).flatMap {
      case c: EcsBuildConfig => Some(c)
      case _: K8sBuildConfig => None
      case _: BuildConfigUndefinedType => None
    }
  }

}


