package k8s

import java.io.FileReader

import com.google.inject.{Inject, Singleton}
import io.flow.delta.aws.Util
import io.flow.delta.v0.models.Version
import io.kubernetes.client.Configuration
import io.kubernetes.client.apis.AppsV1Api
import io.kubernetes.client.models.V1ReplicaSet
import io.kubernetes.client.util.{ClientBuilder, KubeConfig}

import scala.collection.JavaConverters._


@Singleton
class KubernetesService @Inject()(configuration: play.api.Configuration) {

  private val kubeConfigPath = configuration.get[String]("kube.config.path");

  private val kubeConfig = KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))

  private val client = ClientBuilder.kubeconfig(kubeConfig).build();

  Configuration.setDefaultApiClient(client);

  private val apps = new AppsV1Api()

  private val ProductionNamespace = "production"

  def getDeploymentVersion(serviceName: String): Seq[Version] = {
    apps
      .listNamespacedReplicaSet(ProductionNamespace, true, null, null, null, null, null, null, null, false)
      .getItems
      .asScala
      .filter(_.getMetadata.getName.startsWith(serviceName))
      .flatMap(toVersion)
      .filter(_.instances > 0)
  }

  def toVersion(rs: V1ReplicaSet): Seq[Version] = {
    rs.getSpec.getTemplate.getSpec.getContainers.asScala.map(_.getImage).flatMap(Util.parseImage).map { image =>
      Version(
        name = image.version,
        instances = rs.getStatus.getReadyReplicas.toLong,
      )
    }
  }

}
