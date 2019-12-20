package k8s

import java.io.FileReader

import com.google.inject.{Inject, Singleton}
import io.flow.delta.aws.Util
import io.flow.delta.v0.models.Version
import io.kubernetes.client.apis.AppsV1Api
import io.kubernetes.client.models.V1ReplicaSet
import io.kubernetes.client.util.{ClientBuilder, KubeConfig}

import scala.collection.JavaConverters._

trait KubernetesService {

  def getDesiredReplicas(serviceName: String, version: String): Option[Long]

  def getDeployedVersions(serviceName: String): Seq[Version]

}

class MockK8sService extends KubernetesService {

  override def getDeployedVersions(serviceName: String): Seq[Version] = Seq.empty

  override def getDesiredReplicas(serviceName: String, version: String): Option[Long] = None
}

@Singleton
class DefaultKubernetesService @Inject()(configuration: play.api.Configuration) extends KubernetesService {

  private val kubeConfigPath = configuration.get[String]("kube.config.path");

  private val kubeConfig = KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))

  private val ProductionNamespace = "production"

  override def getDeployedVersions(serviceName: String): Seq[Version] = {

    val client = ClientBuilder.kubeconfig(kubeConfig).build()

    val apps = new AppsV1Api(client)

    apps
      .listNamespacedReplicaSet(ProductionNamespace, true, null, null, null, null, null, null, null, false)
      .getItems
      .asScala
      .filter(_.getMetadata.getName.startsWith(serviceName))
      .flatMap(toVersion)
      .filter(_.instances > 0)
  }

  override def getDesiredReplicas(serviceName: String, version: String): Option[Long] = {
    val client = ClientBuilder.kubeconfig(kubeConfig).build()
    val apps = new AppsV1Api(client)
    apps
      .listNamespacedReplicaSet(ProductionNamespace, true, null, null, null, null, null, null, null, false)
      .getItems
      .asScala
      .filter(_.getMetadata.getName.startsWith(serviceName))
      .find(_.getSpec.getTemplate.getSpec.getContainers.asScala.map(_.getImage).flatMap(Util.parseImage).exists(_.version == version))
      .map(_.getSpec.getReplicas.toLong)
  }

  private def toVersion(rs: V1ReplicaSet): Seq[Version] = {
    rs.getSpec.getTemplate.getSpec.getContainers.asScala.map(_.getImage).flatMap(Util.parseImage).map { image =>
      Version(
        name = image.version,
        instances = rs.getStatus.getReadyReplicas.toLong,
      )
    }
  }

}
