package io.flow.delta.aws

import akka.actor.ActorSystem
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder
import com.amazonaws.services.elasticloadbalancing.model._
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

  private[this] lazy val client = AmazonElasticLoadBalancingClientBuilder.
    standard().
    withCredentials(new AWSStaticCredentialsProvider(credentials.aws)).
    withClientConfiguration(configuration.aws).
    build()


  def getHealthyInstances(projectId: String): Seq[String] = {
    val loadBalancerName = ElasticLoadBalancer.getLoadBalancerName(projectId)
    logger.fingerprint(this.getClass.getName).withKeyValue("project", projectId).info(s"AWS ElasticLoadBalancer describeInstanceHealth")

    client.describeInstanceHealth(
      new DescribeInstanceHealthRequest().withLoadBalancerName(loadBalancerName)
    ).getInstanceStates.asScala.filter(_.getState == "InService").map(_.getInstanceId)
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
      client.deleteLoadBalancer(new DeleteLoadBalancerRequest().withLoadBalancerName(name))
    } catch {
      case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("project", projectId).withKeyValue("name", name).error(s"Error deleting load balancer", e)
    }
    name
  }

  def createLoadBalancer(settings: Settings, name: String, externalPort: Int) {
    val sslCertificate = if (name.contains("apibuilder")) {
      settings.apibuilderSslCertificateId
    } else {
      settings.elbSslCertificateId
    }

    val https = new Listener()
      .withProtocol("HTTPS") // incoming request should be over https
      .withInstanceProtocol("HTTP") // elb will forward request to individual instance via http (no "s")
      .withLoadBalancerPort(443)
      .withInstancePort(externalPort.toInt)
      .withSSLCertificateId(sslCertificate)

    val elbListeners = Seq(https)

    try {
      logger.fingerprint(this.getClass.getName).withKeyValue("name", name).info(s"AWS ElasticLoadBalancer createLoadBalancer")
      client.createLoadBalancer(
        new CreateLoadBalancerRequest()
          .withLoadBalancerName(name)
          .withListeners(elbListeners.asJava)
          .withSubnets(settings.elbSubnets.asJava)
          .withSecurityGroups(Seq(settings.elbSecurityGroup).asJava)
      )
    } catch {
      case _: DuplicateLoadBalancerNameException => // no-op already exists
    }

    try {
      client.modifyLoadBalancerAttributes(
        new ModifyLoadBalancerAttributesRequest()
          .withLoadBalancerName(name)
          .withLoadBalancerAttributes(
            new LoadBalancerAttributes()
              .withCrossZoneLoadBalancing(
                new CrossZoneLoadBalancing()
                  .withEnabled(settings.elbCrossZoneLoadBalancing)
              )
              .withConnectionDraining(
                new ConnectionDraining()
                  .withEnabled(true)
                  .withTimeout(60)
              )
          )
      )
    } catch {
      case e: Throwable => logger.fingerprint(this.getClass.getName).withKeyValue("name", name).error(s"Error setting ELB connection drain settings", e)
    }
  }

  def configureHealthCheck(name: String, externalPort: Long, healthcheckUrl: String) {
    try {
      logger.fingerprint(this.getClass.getName).withKeyValue("name", name).info(s"AWS ElasticLoadBalancer configureHealthCheck")
      client.configureHealthCheck(
        new ConfigureHealthCheckRequest()
          .withLoadBalancerName(name)
          .withHealthCheck(
            new HealthCheck()
              .withTarget(s"HTTP:$externalPort$healthcheckUrl")
              .withTimeout(25)
              .withInterval(30)
              .withHealthyThreshold(2)
              .withUnhealthyThreshold(4)
          )
      )
    } catch {
      case _: LoadBalancerNotFoundException => sys.error("Cannot find load balancer $name: $e")
    }
  }

}
