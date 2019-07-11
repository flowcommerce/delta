package io.flow.delta.aws

import akka.actor.ActorSystem
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.ecs.EcsAsyncClient
import software.amazon.awssdk.services.ecs.model._
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

  private[this] lazy val client: EcsAsyncClient = EcsAsyncClient.builder.
    credentialsProvider(StaticCredentialsProvider.create(credentials.aws)).
    overrideConfiguration(configuration.aws).
    build


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
        val containerInstanceArns = client.listContainerInstances(ListContainerInstancesRequest.builder.cluster(cluster).build).get.containerInstanceArns
        val result = client.describeContainerInstances(DescribeContainerInstancesRequest.builder.cluster(cluster).containerInstances(containerInstanceArns).build).get
        val badEc2Instances = result.containerInstances.asScala.filter(_.agentConnected == false).map(_.ec2InstanceId)
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
              service.deployments.asScala.headOption match {
                case None => // do nothing
                case Some(deployment) => {
                  // if there are no instances running, desired count is zero,
                  // and has been over 1 day since the last deployment of the service
                  // let's just delete the service
                  val eventDateTime = new DateTime(deployment.createdAt)
                  val oneDayAgo = new DateTime().minusDays(1)

                  if (service.desiredCount == 0 && service.runningCount == 0 && eventDateTime.isBefore(oneDayAgo)) {
                    logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).withKeyValue("service",service.serviceName).info(s"AWS EC2ContainerService deleteService")
                    client.deleteService(DeleteServiceRequest.builder.cluster(cluster).service(service.serviceName).build).get
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
          ListContainerInstancesRequest.builder.
            cluster(cluster).build
        ).get.containerInstanceArns.asScala

        // call update for each container instance
        containerInstanceArns.map{ containerInstanceArn =>
          logger.fingerprint(this.getClass.getName).withKeyValue("cluster",cluster).withKeyValue("project", projectId).info(s"AWS EC2ContainerService updateContainerAgent")
          client.updateContainerAgent(
            UpdateContainerAgentRequest.builder.
              cluster(cluster).
              containerInstance(containerInstanceArn).
              build
          ).get

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
      client.deleteCluster(DeleteClusterRequest.builder.cluster(name).build).get
    } catch {
      case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("cluster",name).withKeyValue("project", projectId).error(s"Error deleting cluster", e)
    }
    name
  }

  def createCluster(projectId: String): String = {
    val name = EC2ContainerService.getClusterName(projectId)
    logger.fingerprint(this.getClass.getName).withKeyValue("cluster",name).withKeyValue("project", projectId).info(s"AWS EC2ContainerService createCluster")

    try {
      val clusterDoesNotExist = client.describeClusters(DescribeClustersRequest.builder.clusters(name).build).get.clusters.isEmpty
      if (clusterDoesNotExist) {
        client.createCluster(CreateClusterRequest.builder.clusterName(name).build).get
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
            val taskArns = client.listTasks(ListTasksRequest.builder.cluster(cluster).serviceName(service.serviceArn).build).get.taskArns

            // get the tasks using the ARNs
            val tasks = client.describeTasks(DescribeTasksRequest.builder.cluster(cluster).tasks(taskArns).build).get.tasks.asScala.toSeq

            // get the final list of versions we have online in the single service in the cluster
            // since multiple versions of the api can run on the same service in the same cluster
            tasks.groupBy { task =>
              // group by task definition's docker image version
              client.describeTaskDefinition(
                DescribeTaskDefinitionRequest.builder.taskDefinition(task.taskDefinitionArn).build
              ).get.taskDefinition.containerDefinitions.asScala.head.image.split(":").last
            }.map { case (version, tasks) =>
              // get the container instances for these tasks
              val containerArns = tasks.map(_.containerInstanceArn).asJava

              // get the container instances for these tasks given arns - and check first if they are healthy
              val serviceInstances = client.describeContainerInstances(
                DescribeContainerInstancesRequest.builder.cluster(cluster).containerInstances(containerArns).build
              ).get.containerInstances.asScala.map(_.ec2InstanceId)

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
        ListServicesRequest.builder.
          cluster(cluster).
          nextToken(nextToken).
          build
      ).get
      serviceArns += result.serviceArns.asScala.toList

      Option(result.nextToken) match {
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
        DescribeServicesRequest.builder.cluster(cluster).services(servicesToDescribe.asJava).build
      ).get.services.asScala.toList

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
      val result = client.listTaskDefinitions(ListTaskDefinitionsRequest.builder.familyPrefix(taskName).build).get
      if (result.taskDefinitionArns.isEmpty) {
        client.registerTaskDefinition(
          RegisterTaskDefinitionRequest.builder
            .family(taskName)
            .containerDefinitions(
              Seq(
                ContainerDefinition.builder
                  .name(containerName)
                  .image(imageName + ":" + imageVersion)
                  .memory(settings.containerMemory) // memory reserved for container should be the same as jvmMemorySetting
                  .ulimits(
                    Seq(
                      Ulimit.builder
                        .name(UlimitName.NOFILE)
                        .softLimit(1000000)
                        .hardLimit(1000000)
                        .build
                    ).asJava
                  )
                  .portMappings(
                    Seq(
                      PortMapping.builder
                        .containerPort(settings.portContainer)
                        .hostPort(settings.portHost)
                        .build
                    ).asJava
                  )
                  .environment(
                    KeyValuePair.builder
                      .name("JAVA_OPTS")
                      .value(jvmMemorySetting)
                      .build
                  )
                  .command(Seq("production").asJava)
                  .build
              ).asJava
            )
            .build
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
        ListTasksRequest.builder.cluster(clusterName).serviceName(serviceName).build
      ).get.taskArns

      val containerInstances = client.describeTasks(
        DescribeTasksRequest.builder.cluster(clusterName).tasks(taskArns).build
      ).get.tasks.asScala.map(_.containerInstanceArn).asJava

      log.info(s"AWS EC2ContainerService describeContainerInstances")
      client.describeContainerInstances(
        DescribeContainerInstancesRequest.builder
        .cluster(clusterName)
        .containerInstances(containerInstances)
        .build
      ).get.containerInstances.asScala.map{containerInstance => containerInstance.ec2InstanceId }
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
      val serviceDesiredCount = desiredCount.toInt
      val minimumHealthyPercent = if (cfg.allowDowntime.getOrElse(false)) {
        0
      } else {
        50
      }

      log.info(s"AWS EC2ContainerService describeServices")
      val resp = client.describeServices(
        DescribeServicesRequest.builder
          .cluster(clusterName)
          .services(Seq(serviceName).asJava)
          .build
      ).get

      if (!resp.failures().isEmpty() ||
          (!resp.services().isEmpty() && resp.services().get(0).status == "INACTIVE")) {
        // If there are failures (because the service doesn't exist)
        // or the service exists but is INACTIVE, then create the service
        log.info(s"AWS EC2ContainerService createOrUpdateService")
        client.createService(
          // MaximumPercent is set to 200 to allow services with only 1 
          // instance to be deployed with ECS.
          CreateServiceRequest.builder
            .serviceName(serviceName)
            .cluster(clusterName)
            .desiredCount(serviceDesiredCount)
            .role(settings.serviceRole)
            .taskDefinition(taskDefinition)
            .deploymentConfiguration(
              DeploymentConfiguration.builder
                .minimumHealthyPercent(minimumHealthyPercent)
                .maximumPercent(200)
                .build
            )
            .loadBalancers(
            Seq(
              LoadBalancer.builder
                .containerName(containerName)
                .loadBalancerName(loadBalancerName)
                .containerPort(settings.portContainer)
                .build
            ).asJava
          )
          .build
        ).get

      } else {
        // Service exists in cluster, update service task definition
        log.info(s"AWS EC2ContainerService 1.1 createOrUpdateService")
        client.updateService(
          UpdateServiceRequest.builder
            .cluster(clusterName)
            .service(serviceName)
            .desiredCount(serviceDesiredCount)
            .taskDefinition(taskDefinition)
            .build
        ).get
      }

      serviceName
    }
  }

  def summary(service: Service): String = {
    val status = service.status
    val running = service.runningCount
    val desired = service.desiredCount
    val pending = service.pendingCount

    s"status[$status] running[$running] desired[$desired] pending[$pending]"
  }


}
