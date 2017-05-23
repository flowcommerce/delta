package io.flow.delta.aws

import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import io.flow.delta.v0.models.Version
import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model._

import collection.JavaConverters._
import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.Play.current
import org.joda.time.DateTime

import scala.concurrent.Future

object EC2ContainerService {

  /**
    * Name creation helper functions
    **/
  def getClusterName(projectId: String): String = {
    return s"$projectId-cluster"
  }

}


@javax.inject.Singleton
case class EC2ContainerService @javax.inject.Inject() (
  credentials: Credentials,
  configuration: Configuration,
  elb: ElasticLoadBalancer
) {

  private[this] implicit val executionContext = Akka.system.dispatchers.lookup("ec2-context")

  private[this] lazy val ec2Client = new AmazonEC2Client(credentials.aws, configuration.aws)

  private[this] lazy val client = new AmazonECSClient(credentials.aws, configuration.aws)
  
  /**
   * We are currently on version 1.0. The new version 1.1 changes the ECS 
   * deployment to reuse service definitions, only changing the task 
   * definition to update the docker image. This will move the logic of
   * rolling out a new deploy to ECS instead of Delta. Requirements of
   * your service for version 1.1:
   * 
   * - Docker containers upgraded to 1.12.
   * - HEALTHCHECK statement in Dockerfile.
   * - version=1.1 in .delta file
   * 
   * Once all services have been upgraded to version 1.1, we will remove
   * the split logic and default to 1.1 moving forward.
   */
  private[this] val BuildVersion11 = "1.1"
  
  def getBaseName(imageName: String, imageVersion: Option[String] = None): String = {
    Seq(
      Some(s"${imageName.replaceAll("[/]","-")}"), // flow/registry becomes flow-registry
      imageVersion.map { v => s"${v.replaceAll("[.]","-")}" } // 1.2.3 becomes 1-2-3
    ).flatten.mkString("-")
  }

  def getServiceName(imageName: String, imageVersion: String, settings: Settings): String = {
    if (BuildVersion11 == settings.version) {
      // Use the same service name for version 1.1
      s"${getBaseName(imageName)}-service"
    } else {
      s"${getBaseName(imageName, Some(imageVersion))}-service"
    }
  }

  def getContainerName(imageName: String, imageVersion: String, settings: Settings): String = {
    if (BuildVersion11 == settings.version) {
      // Use the same container name for version 1.1
      s"${getBaseName(imageName)}-container"
    } else {
      s"${getBaseName(imageName, Some(imageVersion))}-container"
    }
  }

  def getTaskName(imageName: String, imageVersion: String): String = {
    s"${getBaseName(imageName, Some(imageVersion))}-task"
  }

  /**
    * Checks health of container instance agents
    */
  def ensureContainerAgentHealth(projectId: String): Future[Unit] = {
    Future {
      val cluster = EC2ContainerService.getClusterName(projectId)
      try {
        val result = client.describeContainerInstances(new DescribeContainerInstancesRequest().withCluster(cluster))
        val badEc2Instances = result.getContainerInstances.asScala.filter(_.getAgentConnected == false).map(_.getEc2InstanceId)
        if (!badEc2Instances.isEmpty) {
          ec2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(badEc2Instances.asJava))
          Logger.info(s"Terminated ec2 instances [${badEc2Instances.mkString(",")}] because of unhealthy ecs agent")
        }
      } catch {
        case e: Throwable => Logger.error(s"Failed ensureContainerAgentHealth cluster [$cluster] - Error: ${e.getMessage}")
      }
    }
  }

  /**
  * Functions that interact with AWS ECS
  **/
  def removeOldServices(projectId: String): Future[Unit] = {
    Future {
      try {
        val cluster = EC2ContainerService.getClusterName(projectId)
        Logger.info(s"AWS EC2ContainerService listServices projectId[$projectId]")

        val serviceArns = getServiceArns(cluster)
        serviceArns match {
          case Nil => // do nothing
          case arns => {
            getServicesInfo(cluster, arns).map { service =>
              service.getDeployments.asScala.headOption match {
                case None => // do nothing
                case Some(deployment) => {
                  // if there are no instances running, desired count is zero,
                  // and has been over 1 day since the last deployment of the service
                  // let's just delete the service
                  val eventDateTime = new DateTime(deployment.getCreatedAt)
                  val oneDayAgo = new DateTime().minusDays(1)

                  if (service.getDesiredCount == 0 && service.getRunningCount == 0 && eventDateTime.isBefore(oneDayAgo)) {
                    Logger.info(s"AWS EC2ContainerService deleteService projectId[$projectId], service: ${service.getServiceName}")
                    client.deleteService(new DeleteServiceRequest().withCluster(cluster).withService(service.getServiceName))
                  }
                }
              }
            }
          }
        }
      } catch {
        case e: Throwable => sys.error(s"Removing old services for $projectId: $e")
      }
    }
  }

  def updateContainerAgent(projectId: String): Future[Seq[String]] = {
    Future {
      try {
        val cluster = EC2ContainerService.getClusterName(projectId)

        // find all the container instances for this cluster
        Logger.info(s"AWS EC2ContainerService listContainerInstances projectId[$projectId]")
        val containerInstanceArns = client.listContainerInstances(
          new ListContainerInstancesRequest()
          .withCluster(cluster)
        ).getContainerInstanceArns.asScala

        // call update for each container instance
        containerInstanceArns.map{ containerInstanceArn =>
          Logger.info(s"AWS EC2ContainerService updateContainerAgent projectId[$projectId]")
          val result = client.updateContainerAgent(
            new UpdateContainerAgentRequest()
            .withCluster(cluster)
            .withContainerInstance(containerInstanceArn)
          )

          containerInstanceArn
        }
      } catch {
        case e: UpdateInProgressException => Nil
        case e: NoUpdateAvailableException => Nil
        case e: Throwable => sys.error(s"Error upgrading container agent for $projectId: $e")
      }
    }
  }

  def deleteCluster(projectId: String): String = {
    val name = EC2ContainerService.getClusterName(projectId)
    Logger.info(s"AWS EC2ContainerService deleteCluster projectId[$projectId]")

    try {
      client.deleteCluster(new DeleteClusterRequest().withCluster(name))
    } catch {
      case e: Throwable => Logger.error(s"Error deleting cluster $name - Error ${e.getMessage}")
    }
    name
  }

  def createCluster(projectId: String): String = {
    val name = EC2ContainerService.getClusterName(projectId)
    Logger.info(s"AWS EC2ContainerService createCluster projectId[$projectId]")

    try {
      client.createCluster(new CreateClusterRequest().withClusterName(name))
    } catch {
      case e: Throwable => Logger.error(s"Error creating cluster $name - Error ${e.getMessage}")
    }

    name
  }

  // scale to the desired count - can be up or down
  def scale(
    settings: Settings,
    imageName: String,
    imageVersion: String,
    projectId: String,
    desiredCount: Long
  ): Future[Unit] = {
    for {
      taskDef <- registerTaskDefinition(settings, imageName, imageVersion, projectId)
      service <- createOrUpdateService(settings, imageName, imageVersion, projectId, taskDef, desiredCount)
      count <- updateServiceDesiredCount(settings, imageName, imageVersion, projectId, desiredCount)
    } yield {
      // Nothing
    }
  }

  def updateServiceDesiredCount(
    settings: Settings,
    imageName: String,
    imageVersion: String,
    projectId: String,
    desiredCount: Long
  ): Future[Long] = {
    Future {
      // Find matching service running specified image version and update
      // the desired count.
      val cluster = EC2ContainerService.getClusterName(projectId)
      getClusterInfo(projectId).map { versions =>
        val version = versions.filter(_.name == imageVersion).head     
        if (version != Nil && version.serviceName != Nil) {
          Logger.info(s"AWS EC2ContainerService updateService cluster[$cluster]")
          client.updateService(
            new UpdateServiceRequest()
              .withCluster(cluster)
              .withService(version.serviceName.get)
              .withDesiredCount(desiredCount.toInt)
          )
        }
      }
      desiredCount
    }
  }

  /**
    * Wrapper class that will call fetch healthy instances on first
    * call to instances or contains, caching result thereafter.
    */
  private[this] case class ElbHealthyInstances(projectId: String) {

    private[this] var initialized = false
    private[this] var instances: Seq[String] = Nil

    def instances(): Seq[String] = {
      this.synchronized {
        if (!initialized) {
          instances = elb.getHealthyInstances(projectId)
          initialized = true
        }
        instances
      }
    }

    def contains(name: String): Boolean = {
      instances().contains(name)
    }

  }

  def getClusterInfo(projectId: String): Future[Seq[Version]] = {
    Future {
      val elbHealthyInstances = ElbHealthyInstances(projectId)
      val cluster = EC2ContainerService.getClusterName(projectId)
      val serviceArns = getServiceArns(cluster)

      serviceArns match {
        case Nil => Nil
        case arns => {
          Logger.info(s"AWS EC2ContainerService describeServices projectId[$projectId] serviceArns[${serviceArns.mkString(", ")}]")

          getServicesInfo(cluster, arns).map { service =>
            Logger.info(s"AWS EC2ContainerService describeTaskDefinition projectId[$projectId] taskDefinition[${service.getTaskDefinition}]")
            client.describeTaskDefinition(
              new DescribeTaskDefinitionRequest()
                .withTaskDefinition(service.getTaskDefinition)
            ).getTaskDefinition().getContainerDefinitions().asScala.headOption match {
              case None => {
                sys.error(s"No container definitions for task definition ${service.getTaskDefinition}")
              }

              case Some(containerDef) => {
                val image = Util.parseImage(containerDef.getImage()).getOrElse {
                  sys.error(s"Invalid image name[${containerDef.getImage()}] - could not parse version")
                }

                /**
                  * service.runningCount is what ECS thinks is running but we need to verify that the actual EC2 instances are healthy
                  * Steps:
                  * 1. Get the ECS container instances for this service
                  * 2. Check the ELB if these instances are actually healthy from the ELB
                  */
                Logger.info(s"AWS EC2ContainerService describeTasks - get ECS container instances - projectId[$projectId] service[${service.getServiceArn}]")
                client.listTasks(new ListTasksRequest().withCluster(cluster).withServiceName(service.getServiceArn)).getTaskArns.asScala.toList match {
                  case Nil => {
                    Version(image.version, 0, Some(service.getServiceName))
                  }

                  case taskArns => {
                    client.describeTasks(
                      new DescribeTasksRequest().withCluster(cluster).withTasks(taskArns.asJava)
                    ).getTasks.asScala.map(_.getContainerInstanceArn).toList match {
                      case Nil => {
                        Version(image.version, 0, Some(service.getServiceName))
                      }

                      case serviceContainerInstances => {
                        Logger.info(s"AWS EC2ContainerService describeContainerInstances - get ec2 instance IDs for - projectId[$projectId] serviceContainerInstances[${serviceContainerInstances.mkString(", ")}]")
                        val serviceInstances = client.describeContainerInstances(
                          new DescribeContainerInstancesRequest().withCluster(cluster).withContainerInstances(serviceContainerInstances.asJava)
                        ).getContainerInstances().asScala.map(_.getEc2InstanceId)

                        // healthy instances = count of service instances actually in the elb which are healthy
                        val healthyInstances = serviceInstances.filter(elbHealthyInstances.contains(_))

                        Logger.info(s"  - $projectId: elbInstances: ${elbHealthyInstances.instances().sorted}")
                        Logger.info(s"  - $projectId: serviceInstances: ${serviceInstances.sorted}")
                        Logger.info(s"  - $projectId: healthyInstances: ${healthyInstances.sorted}")
                        Version(image.version, healthyInstances.size, Some(service.getServiceName))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private[this] def getServiceArns(cluster: String): Seq[String] = {
    var serviceArns = scala.collection.mutable.ListBuffer.empty[List[String]]
    var hasMore = true
    var nextToken: String = null // null nextToken gets the first page

    while (hasMore) {
      Logger.info(s"AWS EC2ContainerService listServices cluster[$cluster] nextToken[$nextToken]")
      var result = client.listServices(
        new ListServicesRequest()
          .withCluster(cluster)
          .withNextToken(nextToken)
      )
      serviceArns += result.getServiceArns.asScala.toList

      Option(result.getNextToken) match {
        case Some(token) => {
          nextToken = token
        }
        case None => {
          hasMore = false
        }
      }
    }
    serviceArns.flatten.distinct
  }

  private[this] def getServicesInfo(cluster: String, serviceNames: Seq[String]): Seq[Service] = {
    // describe services 10 at a time
    var services = scala.collection.mutable.ListBuffer.empty[List[Service]]
    val batchSize = 10
    var dropped = 0
    var servicesToDescribe = serviceNames.take(batchSize)

    while (!servicesToDescribe.isEmpty) {
      Logger.info(s"AWS EC2ContainerService getServicesInfo cluster[$cluster], services[${servicesToDescribe.mkString(", ")}]")
      services += client.describeServices(
        new DescribeServicesRequest().withCluster(cluster).withServices(servicesToDescribe.asJava)
      ).getServices().asScala.toList

      dropped += batchSize
      servicesToDescribe = serviceNames.drop(dropped).take(batchSize)
    }

    services.flatten.distinct
  }

  def registerTaskDefinition(
    settings: Settings,
    imageName: String,
    imageVersion: String,
    projectId: String
  ): Future[String] = {
    val taskName = getTaskName(imageName, imageVersion)
    val containerName = getContainerName(imageName, imageVersion, settings)

    Logger.info(s"AWS EC2ContainerService registerTaskDefinition projectId[$projectId] imageName[$imageName] imageVersion[$imageVersion]")

    // bin/[service]-[api|www] script generated by "sbt stage" uses this when passed to JAVA_OPTS below
    val jvmMemorySetting = s"-Xms${settings.containerMemory}m -Xmx${settings.containerMemory}m"

    Future {
      client.registerTaskDefinition(
        new RegisterTaskDefinitionRequest()
          .withFamily(taskName)
          .withContainerDefinitions(
          Seq(
            new ContainerDefinition()
              .withName(containerName)
              .withImage(imageName + ":" + imageVersion)
              .withMemory(settings.containerMemory)
              .withPortMappings(
                Seq(
                  new PortMapping()
                    .withContainerPort(settings.portContainer)
                    .withHostPort(settings.portHost)
                ).asJava
              )
              .withEnvironment(
                new KeyValuePair()
                  .withName("JAVA_OPTS")
                  .withValue(jvmMemorySetting)
              )
              .withCommand(Seq("production").asJava)
          ).asJava
        )
      )

      taskName
    }
  }

  def getServiceInstances(imageName: String, imageVersion: String, projectId: String, settings: Settings): Future[Seq[String]] = {
    val clusterName = EC2ContainerService.getClusterName(projectId)
    val serviceName = getServiceName(imageName, imageVersion, settings)

    Future {
      Logger.info(s"AWS EC2ContainerService describeTasks projectId[$projectId] imageName[$imageName] imageVersion[$imageVersion]")
      val taskArns = client.listTasks(
        new ListTasksRequest().withCluster(clusterName).withServiceName(serviceName)
      ).getTaskArns

      val containerInstances = client.describeTasks(
        new DescribeTasksRequest().withCluster(clusterName).withTasks(taskArns)
      ).getTasks.asScala.map(_.getContainerInstanceArn).asJava

      Logger.info(s"AWS EC2ContainerService describeContainerInstances projectId[$projectId] imageName[$imageName] imageVersion[$imageVersion]")
      client.describeContainerInstances(
        new DescribeContainerInstancesRequest()
        .withCluster(clusterName)
        .withContainerInstances(containerInstances)
      ).getContainerInstances().asScala.map{containerInstance => containerInstance.getEc2InstanceId }
    }
  }

  def createOrUpdateService(
    settings: Settings,
    imageName: String,
    imageVersion: String,
    projectId: String,
    taskDefinition: String,
    desiredCount: Long
  ): Future[String] = {
    Future {
      val clusterName = EC2ContainerService.getClusterName(projectId)
      val serviceName = getServiceName(imageName, imageVersion, settings)
      val containerName = getContainerName(imageName, imageVersion, settings)
      val loadBalancerName = ElasticLoadBalancer.getLoadBalancerName(projectId)

      val (serviceDesiredCount,minimumHealthyPercent) = if (BuildVersion11 == settings.version) {
        (desiredCount.toInt, 50) // allows ECS to deploy new task definitions
      } else {
        (0, 99) // initialize service with desired count of 0, scale up will come later
      }

      Logger.info(s"AWS EC2ContainerService describeServices projectId[$projectId] imageName[$imageName] imageVersion[$imageVersion]")
      val resp = client.describeServices(
        new DescribeServicesRequest()
          .withCluster(clusterName)
          .withServices(Seq(serviceName).asJava)
      )

      if (!resp.getFailures().isEmpty()) {
        // service doesn't exist in cluster, create service
        Logger.info(s"AWS EC2ContainerService createOrUpdateService projectId[$projectId] imageName[$imageName] imageVersion[$imageVersion]")
        client.createService(
          new CreateServiceRequest()
            .withServiceName(serviceName)
            .withCluster(clusterName)
            .withDesiredCount(serviceDesiredCount)
            .withRole(settings.serviceRole)
            .withTaskDefinition(taskDefinition)
            .withDeploymentConfiguration(
            new DeploymentConfiguration()
              .withMinimumHealthyPercent(minimumHealthyPercent)
              .withMaximumPercent(100)
          )
            .withLoadBalancers(
            Seq(
              new LoadBalancer()
                .withContainerName(containerName)
                .withLoadBalancerName(loadBalancerName)
                .withContainerPort(settings.portContainer)
            ).asJava
          )
        )

      } else if (BuildVersion11 == settings.version) {
        // Service exists in cluster, update service but only for version 1.1
        // to make sure we don't mess with existing deploy method
        Logger.info(s"AWS EC2ContainerService 1.1 createOrUpdateService projectId[$projectId] imageName[$imageName] imageVersion[$imageVersion]")
        if (desiredCount > 0) {
          client.updateService(
            new UpdateServiceRequest()
              .withCluster(clusterName)
              .withService(serviceName)
              .withTaskDefinition(taskDefinition)
          )
        } else {
          // Scale down does not apply for delta version 1.1, do nothing
        }
      }

      serviceName
    }
  }

  def summary(service: Service): String = {
    val status = service.getStatus
    val running = service.getRunningCount
    val desired = service.getDesiredCount
    val pending = service.getPendingCount

    s"status[$status] running[$running] desired[$desired] pending[$pending]"
  }


}
