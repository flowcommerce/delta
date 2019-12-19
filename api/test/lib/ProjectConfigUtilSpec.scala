package lib

import io.flow.delta.config.v0.models.Cluster
import io.flow.delta.config.v0.models.json._
import io.flow.delta.lib.BuildNames
import io.flow.delta.lib.config.Defaults
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ProjectConfigUtilSpec extends PlaySpec {

  "parse config" in {
    ProjectConfigUtil.cluster(Defaults.K8sConfig, BuildNames.DefaultBuildName) must be(Some(Cluster.K8s))
    ProjectConfigUtil.cluster(Defaults.K8sConfig, "random") must be(None)

    ProjectConfigUtil.cluster(Defaults.EcsConfig, BuildNames.DefaultBuildName) must be(Some(Cluster.Ecs))
    ProjectConfigUtil.cluster(Defaults.EcsConfig, "random") must be(None)
  }

  "parse valid json string" in {
    ProjectConfigUtil.cluster(Json.toJson(Defaults.K8sConfig).toString, BuildNames.DefaultBuildName) must be(Some(Cluster.K8s))
  }

  "parse invalid json string" in {
    ProjectConfigUtil.cluster("bad json", BuildNames.DefaultBuildName) must be(None)
  }

}
