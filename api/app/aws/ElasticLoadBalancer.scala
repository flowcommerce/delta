package io.flow.delta.aws

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.elasticloadbalancing.model._

import collection.JavaConverters._

import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.Play.current
import scala.concurrent.Future

object ElasticLoadBalancer {

  def getLoadBalancerName(projectId: String): String = s"${projectId.replaceAll("_", "-")}-ecs-lb"

}

@javax.inject.Singleton
case class ElasticLoadBalancer @javax.inject.Inject() (
  credentials: Credentials,
  configuration: Configuration
) {

  private[this] implicit val executionContext = Akka.system.dispatchers.lookup("ec2-context")

  private[this] lazy val client = new AmazonElasticLoadBalancingClient(credentials.aws, configuration.aws)

  def getHealthyInstances(projectId: String): Seq[String] = {
    val loadBalancerName = ElasticLoadBalancer.getLoadBalancerName(projectId)
    Logger.info(s"AWS ElasticLoadBalancer describeInstanceHealth projectId[$projectId]")
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
    Logger.info(s"AWS delete load balancer projectId[$projectId]")

    try {
      client.deleteLoadBalancer(new DeleteLoadBalancerRequest().withLoadBalancerName(name))
    } catch {
      case e: Throwable => Logger.error(s"Error deleting load balancer $name - Error: ${e.getMessage}")
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
      Logger.info(s"AWS ElasticLoadBalancer createLoadBalancer name[$name]")
      client.createLoadBalancer(
        new CreateLoadBalancerRequest()
          .withLoadBalancerName(name)
          .withListeners(elbListeners.asJava)
          .withSubnets(settings.elbSubnets.asJava)
          .withSecurityGroups(Seq(settings.elbSecurityGroup).asJava)
      )
    } catch {
      case e: DuplicateLoadBalancerNameException => {

      }
    }

    try {
      client.modifyLoadBalancerAttributes(
        new ModifyLoadBalancerAttributesRequest()
          .withLoadBalancerName(name)
          .withLoadBalancerAttributes(
            new LoadBalancerAttributes()
              .withCrossZoneLoadBalancing(
                new CrossZoneLoadBalancing()
                  .withEnabled(false)
              )
              .withConnectionDraining(
                new ConnectionDraining()
                  .withEnabled(true)
                  .withTimeout(60)
              )
          )
      )
    } catch {
      case e: Throwable => Logger.error(s"Error setting ELB connection drain settings: ${e.getMessage}")
    }
  }

  def configureHealthCheck(name: String, externalPort: Long, healthcheckUrl: String) {
    try {
      Logger.info(s"AWS ElasticLoadBalancer configureHealthCheck name[$name]")
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
      case e: LoadBalancerNotFoundException => sys.error("Cannot find load balancer $name: $e")
    }
  }

}
