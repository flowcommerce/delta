package lib

import io.flow.delta.config.v0.models.{BuildConfigUndefinedType, Cluster, Config, ConfigError, ConfigProject, ConfigUndefinedType, EcsBuildConfig, K8sBuildConfig}
import io.flow.delta.config.v0.models.json._
import play.api.libs.json.Json

import scala.util.{Failure, Success, Try}

object ProjectConfigUtil {

  val Unknown = Cluster.UNDEFINED("unknown")

  def cluster(config: String, buildName: String): Option[Cluster] = {
    Try {
      Json.parse(config).asOpt[Config].flatMap { c =>
        cluster(c, buildName)
      }
    } match {
      case Success(r) => r
      case Failure(_) => None
    }
  }

  def cluster(config: Config, buildName: String): Option[Cluster] = {
    config match {
      case _: ConfigUndefinedType => None
      case _: ConfigError => None
      case p: ConfigProject => {
        BuildConfigUtil.findBuildByName(p.builds, buildName).map {
          case b: EcsBuildConfig => b.cluster.getOrElse(Cluster.Ecs)
          case b: K8sBuildConfig => b.cluster
          case BuildConfigUndefinedType(_) => Unknown
        }
      }
    }
  }
}
