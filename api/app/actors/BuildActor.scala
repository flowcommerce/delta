package io.flow.delta.actors

import db.{ConfigsDao, UsersDao, BuildLastStatesWriteDao}
import io.flow.delta.aws.{AutoScalingGroup, DefaultSettings, EC2ContainerService, ElasticLoadBalancer}
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.lib.{BuildNames, StateFormatter, Text}
import io.flow.delta.v0.models.{Build, Docker, StateForm}
import io.flow.delta.config.v0.models.BuildStage
import io.flow.play.actors.ErrorHandler
import io.flow.play.util.Config
import play.api.Logger
import akka.actor.{Actor, ActorSystem}
import scala.concurrent.duration._
import scala.concurrent.Future

object BuildActor {

  val CheckLastStateIntervalSeconds = 45
  val ScaleIntervalSeconds = 5

  trait Message

  object Messages {
    case object CheckLastState extends Message

    case object ConfigureAWS extends Message // One-time AWS setup

    case class Scale(diffs: Seq[StateDiff]) extends Message

    case object Setup extends Message

    case object EnsureContainerAgentHealth extends Message

    case object UpdateContainerAgent extends Message

    case object RemoveOldServices extends Message

    case object Delete extends Message
  }

  trait Factory {
    def apply(buildId: String): Actor
  }

}

class BuildActor @javax.inject.Inject() (
  config: Config,
  system: ActorSystem,
  buildLastStatesWriteDao: BuildLastStatesWriteDao,
  override val configsDao: ConfigsDao,
  asg: AutoScalingGroup,
  ecs: EC2ContainerService,
  elb: ElasticLoadBalancer,
  @com.google.inject.assistedinject.Assisted buildId: String
) extends Actor with ErrorHandler with DataBuild with BuildEventLog {

  implicit private[this] val ec = system.dispatchers.lookup("build-actor-context")

  private[this] val TimeoutSeconds = 450


  def receive = {

    case msg @ BuildActor.Messages.Setup => withErrorHandler(msg) {
      setBuildId(buildId)

      if (isScaleEnabled) {
        self ! BuildActor.Messages.ConfigureAWS

        system.scheduler.schedule(
          Duration(1, "second"),
          Duration(BuildActor.CheckLastStateIntervalSeconds, "seconds")
        ) {
          self ! BuildActor.Messages.CheckLastState
        }
      }
    }

    case msg @ BuildActor.Messages.Delete => withErrorHandler(msg) {
      withBuild { build =>
        //removeAwsResources(build)
        Logger.info(s"Called BuildActor.Messages.Delete for build id - ${build.id}, name - ${build.name}")
      }
    }

    case msg @ BuildActor.Messages.CheckLastState => withErrorHandler(msg) {
      withEnabledBuild { build =>
        captureLastState(build)
      }
    }

    case msg @ BuildActor.Messages.EnsureContainerAgentHealth => withErrorHandler(msg) {
      withEnabledBuild { build =>
        ensureContainerAgentHealth(build)
      }
    }

    case msg @ BuildActor.Messages.UpdateContainerAgent => withErrorHandler(msg) {
      withEnabledBuild { build =>
        updateContainerAgent(build)
      }
    }

    case msg @ BuildActor.Messages.RemoveOldServices => withErrorHandler(msg) {
      withEnabledBuild { build =>
        removeOldServices(build)
      }
    }

    // Configure EC2 LC, ELB, ASG for a build (id: user, fulfillment, splashpage, etc)
    case msg @ BuildActor.Messages.ConfigureAWS => withErrorHandler(msg) {
      withEnabledBuild { build =>
        configureAWS(build)
      }
    }

    case msg @ BuildActor.Messages.Scale(diffs) => withErrorHandler(msg) {
      withOrganization { org =>
        withEnabledBuild { build =>
          diffs.foreach { diff =>
            scale(org.docker, build, diff)
          }
        }
      }
    }

    case msg: Any => logUnhandledMessage(msg)

  }

  private[this] def isScaleEnabled(): Boolean = {
    withBuildConfig { buildConfig =>
      buildConfig.stages.contains(BuildStage.Scale)
    }.getOrElse(false)
  }

  def removeAwsResources(build: Build): Future[Unit] = {
    log.runAsync(s"removeAwsResources(${BuildNames.projectName(build)})") {
      for {
        cluster <- deleteCluster(build)
        asg <- deleteAutoScalingGroup(build)
        elb <- deleteLoadBalancer(build)
        lc <- deleteLaunchConfiguration(build)
      } yield {
        Logger.info(s"Deleted cluster $cluster, autoscaling group $asg, elb $elb, launch config $lc")
      }
    }
  }

  def deleteCluster(build: Build): Future[String] = {
    log.runSync("Deleting cluster") {
      ecs.deleteCluster(BuildNames.projectName(build))
    }
  }

  def deleteAutoScalingGroup(build: Build): Future[String] = {
    log.runSync("Deleting ASG") {
      asg.deleteAutoScalingGroup(BuildNames.projectName(build))
    }
  }

  def deleteLoadBalancer(build: Build): Future[String] = {
    log.runSync("Deleting ELB") {
      elb.deleteLoadBalancer(BuildNames.projectName(build))
    }
  }

  def deleteLaunchConfiguration(build: Build): Future[String] = {
    log.runSync("Deleting launch configuration") {
      asg.deleteLaunchConfiguration(awsSettings, BuildNames.projectName(build))
    }
  }

  def configureAWS(build: Build): Future[Unit] = {
    log.runAsync("configureAWS") {
      for {
        cluster <- createCluster(build)
        lc <- createLaunchConfiguration(build)
        elb <- createLoadBalancer(build)
        asg <- upsertAutoScalingGroup(build, lc, elb)
      } yield {
        // All steps have completed
      }
    }
  }

  def ensureContainerAgentHealth(build: Build): Unit = {
    log.runAsync("ECS ensure container agent health") {
      ecs.ensureContainerAgentHealth(BuildNames.projectName(build))
    }
  }

  def updateContainerAgent(build: Build) {
    log.runAsync("ECS updating container agent") {
      ecs.updateContainerAgent(BuildNames.projectName(build))
    }
  }

  def removeOldServices(build: Build): Unit = {
    log.runAsync("ECS cleanup old services") {
      ecs.removeOldServices(BuildNames.projectName(build))
    }
  }

  def scale(docker: Docker, build: Build, diff: StateDiff) {
    val projectName = BuildNames.projectName(build)
    val imageName = BuildNames.dockerImageName(docker, build)
    val imageVersion = diff.versionName

    // only need to run scale once with delta 1.1
    if (diff.lastInstances == 0) {
      self ! BuildActor.Messages.ConfigureAWS
      val instances = diff.desiredInstances - diff.lastInstances

      Logger.info(s"PaoloDeltaDebug project ${build.name}, scale up ${diff.desiredInstances} of ${diff.versionName}")

      log.runAsync(s"Bring up ${Text.pluralize(diff.desiredInstances, "instance", "instances")} of ${diff.versionName}") {
        ecs.scale(awsSettings, imageName, imageVersion, projectName, diff.desiredInstances)
      }
    }
  }

  def captureLastState(build: Build): Future[String] = {
    Logger.info(s"BuildActor[$buildId] captureLastState this.id[$this]")
    ecs.getClusterInfo(BuildNames.projectName(build)).map { versions =>
      buildLastStatesWriteDao.upsert(
        UsersDao.systemUser,
        build,
        StateForm(versions = versions)
      )
      StateFormatter.label(versions)
    }
  }

  def createLaunchConfiguration(build: Build): Future[String] = {
    log.runSync("EC2 auto scaling group launch configuration") {
      asg.createLaunchConfiguration(awsSettings, BuildNames.projectName(build))
    }
  }

  def createLoadBalancer(build: Build): Future[String] = {
    log.runAsync("EC2 load balancer") {
      elb.createLoadBalancerAndHealthCheck(awsSettings, BuildNames.projectName(build))
    }
  }

  def upsertAutoScalingGroup(build: Build, launchConfigName: String, loadBalancerName: String): Future[String] = {
    log.runSync("EC2 auto scaling group") {
      asg.upsertAutoScalingGroup(awsSettings, BuildNames.projectName(build), launchConfigName, loadBalancerName)
    }
  }

  def createCluster(build: Build): Future[String] = {
    log.runSync("Create cluster") {
       ecs.createCluster(BuildNames.projectName(build))
    }
  }

  private[this] def awsSettings() = withBuildConfig { bc =>
    DefaultSettings(
      asgHealthCheckGracePeriod = config.requiredInt("aws.asg.healthcheck.grace.period"),
      asgMinSize = config.requiredInt("aws.asg.min.size"),
      asgMaxSize = config.requiredInt("aws.asg.max.size"),
      asgDesiredSize = config.requiredInt("aws.asg.desired.size"),
      elbSslCertificateId = config.requiredString("aws.elb.ssl.certificate"),
      apibuilderSslCertificateId = config.requiredString("aws.elb.ssl.certificate.apibuilder"),
      elbSubnets = config.requiredString("aws.elb.subnets").split(","),
      asgSubnets = config.requiredString("aws.autoscaling.subnets").split(","),
      lcSecurityGroup = config.requiredString("aws.launch.configuration.security.group"),
      elbSecurityGroup = config.requiredString("aws.service.security.group"),
      ec2KeyName = config.requiredString("aws.service.key"),
      launchConfigImageId = config.requiredString("aws.launch.configuration.ami"),
      launchConfigIamInstanceProfile = config.requiredString("aws.launch.configuration.role"),
      serviceRole = config.requiredString("aws.service.role"),
      instanceType = bc.instanceType,
      containerMemory = bc.memory.asInstanceOf[Int],
      portContainer = bc.portContainer,
      portHost = bc.portHost,
      version = bc.version.getOrElse("1.0")  // default delta version
    )
  }.getOrElse {
    sys.error(s"Build[$buildId] Must have build configuration before getting settings for auto scaling group")
  }

}
