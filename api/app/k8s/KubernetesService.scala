package k8s

import java.io.{File, FileReader}

import com.google.inject.{Inject, Singleton}
import io.flow.delta.aws.Util
import io.flow.delta.v0.models.{Build, Version}
import io.flow.log.RollbarLogger
import io.flow.util.Config
import io.kubernetes.client.apis.AppsV1Api
import io.kubernetes.client.models.V1ReplicaSet
import io.kubernetes.client.util.{ClientBuilder, KubeConfig}

import scala.collection.JavaConverters._

trait KubernetesService {

  def getDesiredReplicaNumber(serviceName: String, version: String): Option[Long]

  def getDeployedVersions(serviceName: String): Seq[Version]

}

class MockK8sService extends KubernetesService {

  override def getDeployedVersions(serviceName: String): Seq[Version] = Seq.empty

  override def getDesiredReplicaNumber(serviceName: String, version: String): Option[Long] = None
}

@Singleton
class DefaultKubernetesService @Inject()(
  config: Config,
  logger: RollbarLogger,
) extends KubernetesService {

  private val ProductionNamespace = "production"

  private val InstanceNameLabel = "app.kubernetes.io/name"

  private[this] val kubeConfig: Option[KubeConfig] = {
    val path = new File(config.requiredString("kube.config.path"))
    if (path.exists()) {
      Some(
        KubeConfig.loadKubeConfig(new FileReader(path))
      )
    } else {
      logger.withKeyValue("path", path.toString).warn("Kube config file not found - k8s will not be available")
      None
    }
  }

  override def getDeployedVersions(serviceName: String): Seq[Version] = {
    replicaSets(serviceName).map(toVersion).filter(_.instances > 0)
  }

  override def getDesiredReplicaNumber(serviceName: String, version: String): Option[Long] = {
    replicaSets(serviceName)
      .find(_.getSpec.getTemplate.getSpec.getContainers.asScala.map(_.getImage).flatMap(Util.parseImage).exists(_.version == version))
      .map(_.getSpec.getReplicas.toLong)
  }

  private[this] def client: Option[AppsV1Api] = {
    kubeConfig match {
      case None => None
      case Some(c) => {
        val client = ClientBuilder.kubeconfig(c).build()
        Some(new AppsV1Api(client))
      }
    }
  }

  private[this] def replicaSets(serviceName: String): Seq[V1ReplicaSet] = {
    client match {
      case None => Nil
      case Some(c) => {
        c.listNamespacedReplicaSet(ProductionNamespace, true, null, null, null, s"$InstanceNameLabel=$serviceName", null, null, null, false)
          .getItems
          .asScala
      }
    }
  }

  private def toVersion(rs: V1ReplicaSet): Version = {
    rs.getSpec.getTemplate.getSpec.getContainers.asScala.map(_.getImage).flatMap(Util.parseImage).map { image =>
      Version(
        name = image.version,
        instances = rs.getStatus.getReadyReplicas.toLong,
      )
    }.head
  }

}

object KubernetesService {

  private val Root = "root"

  def toDeploymentName(build :Build) : String = {
      if (build.name == Root) {
        build.project.id
      } else {
        s"${build.project.id}-${build.name}"
      }
  }

}
