package io.flow.delta.aws

import akka.actor.ActorSystem
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.ecs.model._
import io.flow.delta.lib.BuildNames
import io.flow.delta.v0.models.{Build, Organization, Version}
import io.flow.delta.config.v0.{models => config}
import io.flow.log.RollbarLogger
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.concurrent.Future

object EC2ContainerService {

  /**
    * Name creation helper functions
    **/
  def getClusterName(projectId: String): String =
     s"${projectId.replaceAll("_","-")}-cluster"

}


@javax.inject.Singleton
case class EC2ContainerService @javax.inject.Inject() (
  credentials: Credentials,
  configuration: Configuration,
  elb: ElasticLoadBalancer,
  system: ActorSystem,
  logger: RollbarLogger
) {

  private[this] implicit val executionContext = system.dispatchers.lookup("ec2-context")

  private[this] lazy val client = AmazonECSClientBuilder.
    standard().
    withCredentials(new AWSStaticCredentialsProvider(credentials.aws)).
    withClientConfiguration(configuration.aws).
    build()


  def getBaseName(org: Organization, build: Build, imageVersion: Option[String] = None): String = {
    val projectId = BuildNames.projectName(build)
    Seq(
      Option(org.docker.organization),
      Option(projectId),
      imageVersion.map { v => s"${v.replaceAll("[.]","-")}" } // 1.2.3 becomes 1-2-3
    ).flatten.mkString("-").replaceAll("_", "-")
  }

  private def getServiceName(org: Organization, build: Build): String = {
    s"${getBaseName(org, build)}-service"
  }

  private def getContainerName(org: Organization, build: Build): String = {
    s"${getBaseName(org, build)}-container"
  }

  private def getTaskName(org: Organization, build: Build, imageVersion: String): String = {
    s"${getBaseName(org, build, Some(imageVersion))}-task"
  }

  /**
    * Checks health of container instance agents
    */
  def ensureContainerAgentHealth(projectId: String): Future[Unit] = {
    Future {
      val cluster = EC2ContainerService.getClusterName(projectId)
      logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).info(s"ensureContainerAgentHealth for cluster")
      try {
        val containerInstanceArns = client.listContainerInstances(new ListContainerInstancesRequest().withCluster(cluster)).getContainerInstanceArns
        val result = client.describeContainerInstances(new DescribeContainerInstancesRequest().withCluster(cluster).withContainerInstances(containerInstanceArns))
        val badEc2Instances = result.getContainerInstances.asScala.filter(_.getAgentConnected == false).map(_.getEc2InstanceId)
        if (badEc2Instances.nonEmpty) {
          //ec2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(badEc2Instances.asJava))
          logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).withKeyValue("instances", badEc2Instances).info(s"FlowDeltaError - ensureContainerAgentHealth cluster are unhealthy - please take a look")
        }
      } catch {
        case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).error(s"Failed ensureContainerAgentHealth", e)
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
        logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).info(s"AWS EC2ContainerService listServices")

        val serviceArns = getServiceArns(cluster)
        serviceArns match {
          case Nil => // do nothing
          case arns => {
            getServicesInfo(cluster, arns).foreach { service =>
              service.getDeployments.asScala.headOption match {
                case None => // do nothing
                case Some(deployment) => {
                  // if there are no instances running, desired count is zero,
                  // and has been over 1 day since the last deployment of the service
                  // let's just delete the service
                  val eventDateTime = new DateTime(deployment.getCreatedAt)
                  val oneDayAgo = new DateTime().minusDays(1)

                  if (service.getDesiredCount == 0 && service.getRunningCount == 0 && eventDateTime.isBefore(oneDayAgo)) {
                    logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).withKeyValue("service",service.getServiceName).info(s"AWS EC2ContainerService deleteService")
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
        logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).info(s"AWS EC2ContainerService listContainerInstances")
        val containerInstanceArns = client.listContainerInstances(
          new ListContainerInstancesRequest()
          .withCluster(cluster)
        ).getContainerInstanceArns.asScala

        // call update for each container instance
        containerInstanceArns.map{ containerInstanceArn =>
          logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).info(s"AWS EC2ContainerService updateContainerAgent")
          client.updateContainerAgent(
            new UpdateContainerAgentRequest()
            .withCluster(cluster)
            .withContainerInstance(containerInstanceArn)
          )

          containerInstanceArn
        }
      } catch {
        case _: UpdateInProgressException => Nil
        case _: NoUpdateAvailableException => Nil
        case e: Throwable => sys.error(s"Error upgrading container agent for $projectId: $e")
      }
    }
  }

  def deleteCluster(projectId: String): String = {
    val name = EC2ContainerService.getClusterName(projectId)
    logger.fingerprint(this.getClass.getName).withKeyValue("cluster",name).withKeyValue("project", projectId).info(s"AWS EC2ContainerService deleteCluster")

    try {
      client.deleteCluster(new DeleteClusterRequest().withCluster(name))
    } catch {
      case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("cluster",name).withKeyValue("project", projectId).error(s"Error deleting cluster", e)
    }
    name
  }

  def createCluster(projectId: String): String = {
    val name = EC2ContainerService.getClusterName(projectId)
    logger.fingerprint(this.getClass.getName).withKeyValue("cluster",name).withKeyValue("project", projectId).info(s"AWS EC2ContainerService createCluster")

    try {
      val clusterDoesNotExist = client.describeClusters(new DescribeClustersRequest().withClusters(name)).getClusters.isEmpty
      if (clusterDoesNotExist) {
        client.createCluster(new CreateClusterRequest().withClusterName(name))
      }
    } catch {
      case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("cluster",name).withKeyValue("project", projectId).error(s"Error creating cluster", e)
    }

    name
  }

  // scale to the desired count - can be up or down
  def scale(
    settings: Settings,
    org: Organization,
    build: Build,
    cfg: config.Build,
    imageVersion: String,
    desiredCount: Long
  ): Future[Unit] = {
    for {
      taskDef <- registerTaskDefinition(settings, org, build, cfg, imageVersion)
      _ <- createOrUpdateService(settings, org, build, cfg, imageVersion, taskDef, desiredCount)
    } yield {
      // Nothing
    }
  }

  /**
    * Wrapper class that will call fetch healthy instances on first
    * call to instances or contains, caching result thereafter.
    */
  private[this] case class ElbHealthyInstances(projectId: String) {

    @volatile
    private[this] var initialized = false

    @volatile
    private[this] var instances: Seq[String] = Nil

    def getInstances(): Seq[String] = {
      this.synchronized {
        if (!initialized) {
          instances = elb.getHealthyInstances(projectId)
          initialized = true
        }
        instances
      }
    }

    def contains(name: String): Boolean = {
      getInstances().contains(name)
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
          val versions = getServicesInfo(cluster, arns).flatMap { service =>
            // task ARNs running in the service
            val taskArns = client.listTasks(new ListTasksRequest().withCluster(cluster).withServiceName(service.getServiceArn)).getTaskArns

            // get the tasks using the ARNs
            val tasks = client.describeTasks(new DescribeTasksRequest().withCluster(cluster).withTasks(taskArns)).getTasks.asScala.toSeq

            // get the final list of versions we have online in the single service in the cluster
            // since multiple versions of the api can run on the same service in the same cluster
            tasks.groupBy { task =>
              // group by task definition's docker image version
              client.describeTaskDefinition(
                new DescribeTaskDefinitionRequest().withTaskDefinition(task.getTaskDefinitionArn)
              ).getTaskDefinition.getContainerDefinitions.asScala.head.getImage.split(":").last
            }.map { case (version, tasks) =>
              // get the container instances for these tasks
              val containerArns = tasks.map(_.getContainerInstanceArn).asJava

              // get the container instances for these tasks given arns - and check first if they are healthy
              val serviceInstances = client.describeContainerInstances(
                new DescribeContainerInstancesRequest().withCluster(cluster).withContainerInstances(containerArns)
              ).getContainerInstances.asScala.map(_.getEc2InstanceId)

              // healthy instances = count of service instances actually in the elb which are healthy
              val healthyInstances = serviceInstances.filter(elbHealthyInstances.contains)
              Version(version, healthyInstances.size.toLong)
            }
          }

          versions
        }
      }
    }
  }
  
  private[this] def getServiceArns(cluster: String): Seq[String] = {
    val serviceArns = scala.collection.mutable.ListBuffer.empty[List[String]]
    var hasMore = true
    var nextToken: String = null // null nextToken gets the first page

    while (hasMore) {
      logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("next_token", nextToken).info(s"AWS EC2ContainerService listServices")
      val result = client.listServices(
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
    val services = scala.collection.mutable.ListBuffer.empty[List[Service]]
    val batchSize = 10
    var dropped = 0
    var servicesToDescribe = serviceNames.take(batchSize)

    while (!servicesToDescribe.isEmpty) {
      logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("services", servicesToDescribe).info(s"AWS EC2ContainerService getServicesInfo")
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
    org: Organization,
    build: Build,
    cfg: config.Build,
    imageVersion: String,
  ): Future[String] = {
    val taskName = getTaskName(org, build, imageVersion)
    val containerName = getContainerName(org, build)
    val imageName = BuildNames.dockerImageName(org.docker, build, cfg)

    logger.fingerprint(this.getClass.getName).withKeyValue("task",taskName).withKeyValue("container",containerName).withKeyValue("image", imageName).withKeyValue("image_version", imageVersion).info(s"AWS EC2ContainerService registerTaskDefinition")

    // bin/[service]-[api|www] script generated by "sbt stage" uses this when passed to JAVA_OPTS below
    val jvmMemorySetting = s"-Xms${settings.jvmMemory}m -Xmx${settings.jvmMemory}m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt"

    // if task definition does not exist, register a new one
    Future {
      // taskname = flowcommerce-token-0-1-51-task
      // only register a new task definition if the current task name does not already exist
      val result = client.listTaskDefinitions(new ListTaskDefinitionsRequest().withFamilyPrefix(taskName))
      if (result.getTaskDefinitionArns.isEmpty) {
        client.registerTaskDefinition(
          new RegisterTaskDefinitionRequest()
            .withFamily(taskName)
            .withContainerDefinitions(
              Seq(
                new ContainerDefinition()
                  .withName(containerName)
                  .withImage(imageName + ":" + imageVersion)
                  .withMemory(settings.containerMemory) // memory reserved for container should be the same as jvmMemorySetting
                  .withUlimits(
                    Seq(
                      new Ulimit()
                        .withName(UlimitName.Nofile)
                        .withSoftLimit(1000000)
                        .withHardLimit(1000000)
                    ).asJava
                  )
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
      }

      taskName
    }
  }

  def getServiceInstances(org: Organization, build: Build, imageVersion: String): Future[Seq[String]] = {
    val projectId = BuildNames.projectName(build)
    val clusterName = EC2ContainerService.getClusterName(projectId)
    val serviceName = getServiceName(org, build)
    val log = logger.
      fingerprint(this.getClass.getName).
      withKeyValue("project_id", projectId).
      withKeyValue("build_id", build.id).
      withKeyValue("image_version", imageVersion).
      withKeyValue("cluster_name", clusterName).
      withKeyValue("service_name", serviceName)

    Future {
      log.info(s"AWS EC2ContainerService describeTasks")
      val taskArns = client.listTasks(
        new ListTasksRequest().withCluster(clusterName).withServiceName(serviceName)
      ).getTaskArns

      val containerInstances = client.describeTasks(
        new DescribeTasksRequest().withCluster(clusterName).withTasks(taskArns)
      ).getTasks.asScala.map(_.getContainerInstanceArn).asJava

      log.info(s"AWS EC2ContainerService describeContainerInstances")
      client.describeContainerInstances(
        new DescribeContainerInstancesRequest()
        .withCluster(clusterName)
        .withContainerInstances(containerInstances)
      ).getContainerInstances().asScala.map{containerInstance => containerInstance.getEc2InstanceId }
    }
  }

  def createOrUpdateService(
    settings: Settings,
    org: Organization,
    build: Build,
    cfg: config.Build,
    imageVersion: String,
    taskDefinition: String,
    desiredCount: Long
  ): Future[String] = {
    Future {
      val projectId = BuildNames.projectName(build)
      val clusterName = EC2ContainerService.getClusterName(projectId)
      val serviceName = getServiceName(org, build)
      val containerName = getContainerName(org, build)
      val loadBalancerName = ElasticLoadBalancer.getLoadBalancerName(projectId)
      val log = logger.
        fingerprint(this.getClass.getName).
        withKeyValue("project_id", projectId).
        withKeyValue("build_id", build.id).
        withKeyValue("image_version", imageVersion).
        withKeyValue("cluster_name", clusterName).
        withKeyValue("service_name", serviceName).
        withKeyValue("container_name", containerName).
        withKeyValue("load_balancer_name", loadBalancerName)

      // allows ECS to deploy new task definitions
      val minimumHealthyPercent = if (cfg.allowDowntime.getOrElse(false)) {
        0
      } else {
        /**
         * desired: 1 - min healthy pct: 0
         * desired: 2 - min healthy pct: 50
         * desired: 3 - min healthy pct: 33
         * desired: 4 - min healthy pct: 50
         * desired: 5 - min healthy pct: 60
         * desired: 6 - min healthy pct: 66
         * desired: 7 - min healthy pct: 71
         * desired: 8 - min healthy pct: 75
         * desired: 9 - min healthy pct: 77
         * desired: 10 - min healthy pct: 80
         */
        if (desiredCount <= 2) {
          ((BigDecimal(1) - (BigDecimal(1) / BigDecimal(desiredCount))) * 100).toInt
        } else {
          ((BigDecimal(1) - (BigDecimal(2) / BigDecimal(desiredCount))) * 100).toInt
        }
      }

      log.info(s"AWS EC2ContainerService describeServices")
      val resp = client.describeServices(
        new DescribeServicesRequest()
          .withCluster(clusterName)
          .withServices(Seq(serviceName).asJava)
      )

      val deploymentConfiguration = new DeploymentConfiguration()
        .withMinimumHealthyPercent(minimumHealthyPercent)
        .withMaximumPercent(200)

      if (!resp.getFailures().isEmpty() ||
          (!resp.getServices().isEmpty() && resp.getServices().get(0).getStatus() == "INACTIVE")) {
        // If there are failures (because the service doesn't exist)
        // or the service exists but is INACTIVE, then create the service
        log.info(s"AWS EC2ContainerService createOrUpdateService")
        client.createService(
          // MaximumPercent is set to 200 to allow services with only 1 
          // instance to be deployed with ECS.
          new CreateServiceRequest()
            .withServiceName(serviceName)
            .withCluster(clusterName)
            .withDesiredCount(desiredCount.toInt)
            .withRole(settings.serviceRole)
            .withTaskDefinition(taskDefinition)
            .withDeploymentConfiguration(deploymentConfiguration)
            .withLoadBalancers(
            Seq(
              new LoadBalancer()
                .withContainerName(containerName)
                .withLoadBalancerName(loadBalancerName)
                .withContainerPort(settings.portContainer)
            ).asJava
          )
        )

      } else {
        // Service exists in cluster, update service task definition
        log.info(s"AWS EC2ContainerService 1.1 createOrUpdateService")
        client.updateService(
          new UpdateServiceRequest()
            .withCluster(clusterName)
            .withService(serviceName)
            .withDesiredCount(desiredCount.toInt)
            .withTaskDefinition(taskDefinition)
            .withDeploymentConfiguration(deploymentConfiguration)
        )
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
