package io.flow.delta.aws

import akka.actor.ActorSystem
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingAsyncClient
import software.amazon.awssdk.services.elasticloadbalancing.model._
import io.flow.log.RollbarLogger

import scala.collection.JavaConverters._
import scala.concurrent.Future

object ElasticLoadBalancer {

  def getLoadBalancerName(projectId: String): String = s"${projectId.replaceAll("_", "-")}-ecs-lb"

}

@javax.inject.Singleton
case class ElasticLoadBalancer @javax.inject.Inject() (
  credentials: Credentials,
  configuration: Configuration,
  system: ActorSystem,
  logger: RollbarLogger
) {

  private[this] implicit val executionContext = system.dispatchers.lookup("ec2-context")

  private[this] lazy val client: ElasticLoadBalancingAsyncClient = ElasticLoadBalancingAsyncClient.builder.
    credentialsProvider(StaticCredentialsProvider.create(credentials.aws)).
    overrideConfiguration(configuration.aws).
    build


  def getHealthyInstances(projectId: String): Seq[String] = {
    val loadBalancerName = ElasticLoadBalancer.getLoadBalancerName(projectId)
    logger.fingerprint(this.getClass.getName).withKeyValue("project", projectId).info(s"AWS ElasticLoadBalancer describeInstanceHealth")

    client.describeInstanceHealth(
      DescribeInstanceHealthRequest.builder.loadBalancerName(loadBalancerName).build
    ).get.instanceStates.asScala.filter(_.state == "InService").map(_.instanceId)
  }

  def createLoadBalancerAndHealthCheck(settings: Settings, projectId: String): Future[String] = {
    // create the load balancer first, then configure healthcheck
    // they do not allow this in a single API call
    Future {
      val name = ElasticLoadBalancer.getLoadBalancerName(projectId)
      createLoadBalancer(settings, name, settings.portHost)
      configureHealthCheck(name, settings.portHost, settings.healthcheckUrl)
      name
    }
  }

  def deleteLoadBalancer(projectId: String): String = {
    val name = ElasticLoadBalancer.getLoadBalancerName(projectId)
    logger.fingerprint(this.getClass.getName).withKeyValue("project", projectId).info(s"AWS delete load balancer")

    try {
      client.deleteLoadBalancer(DeleteLoadBalancerRequest.builder.loadBalancerName(name).build).get
    } catch {
      case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("project", projectId).withKeyValue("name", name).error(s"Error deleting load balancer", e)
    }
    name
  }

  def createLoadBalancer(settings: Settings, name: String, externalPort: Int): Unit = {
    val sslCertificate = if (name.contains("apibuilder")) {
      settings.apibuilderSslCertificateId
    } else {
      settings.elbSslCertificateId
    }

    val https = Listener.builder
      .protocol("HTTPS") // incoming request should be over https
      .instanceProtocol("HTTP") // elb will forward request to individual instance via http (no "s")
      .loadBalancerPort(443)
      .instancePort(externalPort.toInt)
      .sslCertificateId(sslCertificate)
      .build

    val elbListeners = Seq(https)

    try {
      logger.fingerprint(this.getClass.getName).withKeyValue("name", name).info(s"AWS ElasticLoadBalancer createLoadBalancer")
      client.createLoadBalancer(
        CreateLoadBalancerRequest.builder
          .loadBalancerName(name)
          .listeners(elbListeners.asJavaCollection)
          .subnets(settings.elbSubnets.asJava)
          .securityGroups(Seq(settings.elbSecurityGroup).asJava)
          .build
      ).get
    } catch {
      case _: DuplicateLoadBalancerNameException => // no-op already exists
    }

    try {
      client.modifyLoadBalancerAttributes(
        ModifyLoadBalancerAttributesRequest.builder
          .loadBalancerName(name)
          .loadBalancerAttributes(
            LoadBalancerAttributes.builder
              .crossZoneLoadBalancing(
                CrossZoneLoadBalancing.builder
                  .enabled(settings.elbCrossZoneLoadBalancing)
                  .build
              )
              .connectionDraining(
                ConnectionDraining.builder
                  .enabled(true)
                  .timeout(60)
                  .build
              )
              .build
          )
          .build
      ).get
      ()
    } catch {
      case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("name", name).error(s"Error setting ELB connection drain settings", e)
    }
  }

  def configureHealthCheck(name: String, externalPort: Int, healthcheckUrl: String): Unit = {
    try {
      logger.fingerprint(this.getClass.getName).withKeyValue("name", name).info(s"AWS ElasticLoadBalancer configureHealthCheck")
      client.configureHealthCheck(
        ConfigureHealthCheckRequest.builder
          .loadBalancerName(name)
          .healthCheck(
            HealthCheck.builder
              .target(s"HTTP:$externalPort$healthcheckUrl")
              .timeout(25)
              .interval(30)
              .healthyThreshold(2)
              .unhealthyThreshold(4)
              .build
          )
          .build
      ).get
      ()
    } catch {
      case e: LoadBalancerNotFoundException => sys.error(s"Cannot find load balancer $name: $e")
    }
  }

}
