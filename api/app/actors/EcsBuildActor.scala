package io.flow.delta.actors

import akka.actor.{Actor, ActorSystem}
import db._
import db.generated.AmiUpdatesDao
import io.flow.akka.SafeReceive
import io.flow.delta.api.lib.{EventLogProcessor, StateDiff}
import io.flow.delta.aws.{AutoScalingGroup, DefaultSettings, EC2ContainerService, ElasticLoadBalancer}
import io.flow.delta.config.v0.models.{BuildStage, EcsBuildConfig}
import io.flow.delta.lib.config.InstanceTypeDefaults
import io.flow.delta.lib.{BuildNames, StateFormatter, Text}
import io.flow.delta.v0.models.{Build, Organization, StateForm}
import io.flow.log.RollbarLogger
import io.flow.util.Config
import io.flow.postgresql.OrderBy

import scala.concurrent.Future
import scala.concurrent.duration._

object EcsBuildActor {

  val CheckLastStateIntervalSeconds = 45L
  val ScaleIntervalSeconds = 5

  trait Message

  object Messages {
    case object ConfigureAWS extends Message // One-time AWS setup

    case class Scale(diffs: Seq[StateDiff]) extends Message

    case object EnsureContainerAgentHealth extends Message

    case object UpdateContainerAgent extends Message

    case object RemoveOldServices extends Message

    case object Delete extends Message
  }

  trait Factory {
    def apply(buildId: String): Actor
  }

}

class EcsBuildActor @javax.inject.Inject() (
  asg: AutoScalingGroup,
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  buildLastStatesDao: InternalBuildLastStatesDao,
  amiUpdatesDao: AmiUpdatesDao,
  config: Config,
  ecs: EC2ContainerService,
  elb: ElasticLoadBalancer,
  eventLogProcessor: EventLogProcessor,
  usersDao: UsersDao,
  system: ActorSystem,
  override val logger: RollbarLogger,
  @com.google.inject.assistedinject.Assisted buildId: String
) extends Actor with DataBuild {

  private[this] implicit val ec = system.dispatchers.lookup("build-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("EcsBuildActor")

  def receive = SafeReceive.withLogUnhandled {

    case BuildActor.Messages.Setup =>
      handleReceiveSetupEvent()

    case EcsBuildActor.Messages.Delete =>
      withBuild { build =>
        //removeAwsResources(build)
        logger.withKeyValue("build_id", build.id).withKeyValue("build_name", build.name).withKeyValue("project", build.project.id).info(s"Called EcsBuildActor.Messages.Delete for build")
      }

    case BuildActor.Messages.CheckLastState =>
      withEnabledBuild { build =>
        captureLastState(build) // Should Await the Future?
      }

    case EcsBuildActor.Messages.EnsureContainerAgentHealth =>
      withEnabledBuild { build =>
        ensureContainerAgentHealth(build) // Should Await the Future?
      }

    case EcsBuildActor.Messages.UpdateContainerAgent =>
      withEnabledBuild { build =>
        updateContainerAgent(build) // Should Await the Future?
      }

    case EcsBuildActor.Messages.RemoveOldServices =>
      withEnabledBuild { build =>
        removeOldServices(build) // Should Await the Future?
      }

    // Configure EC2 LC, ELB, ASG for a build (id: user, fulfillment, splashpage, etc)
    case EcsBuildActor.Messages.ConfigureAWS =>
      withEnabledBuild { build =>
        configureAWS(build) // Should Await the Future?
      }

    case EcsBuildActor.Messages.Scale(diffs) =>
      withOrganization { org =>
        withEnabledBuild { build =>
          diffs.foreach { diff =>
            scale(org, build, requiredEcsBuildConfig, diff) // Should Await the Future?
          }
        }
      }
  }

  private[this] def handleReceiveSetupEvent(): Unit = {
    setBuildId(buildId)

    if (isScaleEnabled) {
      self ! EcsBuildActor.Messages.ConfigureAWS

      system.scheduler.scheduleWithFixedDelay(
        Duration(1L, "second"),
        Duration(EcsBuildActor.CheckLastStateIntervalSeconds, "seconds")
      ) {
        () => self ! BuildActor.Messages.CheckLastState
      }
      ()
    }
  }

  private[this] def isScaleEnabled: Boolean = {
    withEcsBuildConfig { buildConfig =>
      buildConfig.stages.contains(BuildStage.Scale)
    }.getOrElse(false)
  }

  def removeAwsResources(build: Build): Future[Unit] = {
    eventLogProcessor.runAsync(s"removeAwsResources(${BuildNames.projectName(build)})", log = log(build.project.id)) {
      for {
        cluster <- deleteCluster(build)
        asg <- deleteAutoScalingGroup(build)
        elb <- deleteLoadBalancer(build)
        lc <- deleteLaunchConfiguration(build)
      } yield {
        logger.withKeyValue("lc", lc).withKeyValue("elb", elb).withKeyValue("cluster", cluster).withKeyValue("asg", asg).withKeyValue("build_id", build.id).withKeyValue("build_name", build.name).withKeyValue("project", build.project.id).info(s"Deleted AWS resources")
      }
    }
  }

  def deleteCluster(build: Build): Future[String] = {
    eventLogProcessor.runSync("Deleting cluster", log = log(build.project.id)) {
      ecs.deleteCluster(BuildNames.projectName(build))
    }
  }

  def deleteAutoScalingGroup(build: Build): Future[String] = {
    eventLogProcessor.runSync("Deleting ASG", log = log(build.project.id)) {
      asg.delete(BuildNames.projectName(build))
    }
  }

  def deleteLoadBalancer(build: Build): Future[String] = {
    eventLogProcessor.runSync("Deleting ELB", log = log(build.project.id)) {
      elb.deleteLoadBalancer(BuildNames.projectName(build))
    }
  }

  def deleteLaunchConfiguration(build: Build): Future[String] = {
    eventLogProcessor.runSync("Deleting launch configuration", log = log(build.project.id)) {
      asg.deleteLaunchConfiguration(awsSettings, BuildNames.projectName(build))
    }
  }

  def configureAWS(build: Build): Future[Unit] = {
    eventLogProcessor.runAsync("configureAWS", log = log(build.project.id)) {
      for {
        _ <- createCluster(build)
        lc <- createLaunchConfiguration(build)
        elb <- createLoadBalancer(build)
        _ <- upsertAutoScalingGroup(build, lc, elb)
      } yield {
        // All steps have completed
      }
    }
  }

  def ensureContainerAgentHealth(build: Build): Future[Unit] = {
    eventLogProcessor.runAsync("ECS ensure container agent health", log = log(build.project.id)) {
      ecs.ensureContainerAgentHealth(BuildNames.projectName(build))
    }
  }

  def updateContainerAgent(build: Build): Future[Seq[String]] = {
    eventLogProcessor.runAsync("ECS updating container agent", log = log(build.project.id)) {
      ecs.updateContainerAgent(BuildNames.projectName(build))
    }
  }

  def removeOldServices(build: Build): Future[Unit] = {
    eventLogProcessor.runAsync("ECS cleanup old services", log = log(build.project.id)) {
      ecs.removeOldServices(BuildNames.projectName(build))
    }
  }

  def scale(org: Organization, build: Build, cfg: EcsBuildConfig, diff: StateDiff): Future[Unit] = {
    val imageVersion = diff.versionName

    // only need to run scale once with delta 1.1
    if (diff.lastInstances == 0) {
      self ! EcsBuildActor.Messages.ConfigureAWS
      eventLogProcessor.runAsync(s"Bring up ${Text.pluralize(diff.desiredInstances, "instance", "instances")} of ${diff.versionName}", log = log(build.project.id)) {
        ecs.scale(awsSettings, org, build, cfg, imageVersion, diff.desiredInstances)
      }
    } else {
      Future.successful(())
    }
  }

  def captureLastState(build: Build): Future[String] = {
    ecs.getClusterInfo(BuildNames.projectName(build)).map { versions =>
      buildLastStatesDao.upsert(
        usersDao.systemUser,
        build,
        StateForm(versions = versions)
      )
      StateFormatter.label(versions)
    }
  }

  def createLaunchConfiguration(build: Build): Future[String] = {
    eventLogProcessor.runSync("EC2 auto scaling group launch configuration", log = log(build.project.id)) {
      asg.createLaunchConfiguration(awsSettings, BuildNames.projectName(build))
    }
  }

  def createLoadBalancer(build: Build): Future[String] = {
    eventLogProcessor.runAsync("EC2 load balancer", log = log(build.project.id)) {
      elb.createLoadBalancerAndHealthCheck(awsSettings, BuildNames.projectName(build))
    }
  }

  def upsertAutoScalingGroup(build: Build, launchConfigName: String, loadBalancerName: String): Future[String] = {
    eventLogProcessor.runSync("EC2 auto scaling group", log = log(build.project.id)) {
      asg.upsert(awsSettings, BuildNames.projectName(build), launchConfigName, loadBalancerName)
    }
  }

  def createCluster(build: Build): Future[String] = {
    eventLogProcessor.runSync("Create cluster", log = log(build.project.id)) {
      val projectId = BuildNames.projectName(build)
      ecs.createCluster(projectId)
    }
  }

  private[this] def awsSettings(): DefaultSettings = withEcsBuildConfig { bc =>
    val instanceType = bc.instanceType
    val instanceMemorySettings = InstanceTypeDefaults.memory(instanceType)
    val latestAmi = amiUpdatesDao.findAll(limit = Some(1), orderBy = OrderBy("-ami_updates.created_at")).head.id

    // if `memory` passed in to .delta, use that (previously deprecated feature that could still be useful)
    // otherwise default to InstanceTypeDefaults.jvm
    val jvmMemory = bc.memory.map(_.toInt).getOrElse(instanceMemorySettings.jvm)
    val containerMemory = bc.containerMemory.map(_.toInt).getOrElse(instanceMemorySettings.container)

    val ebs = InstanceTypeDefaults.ebs(jvmMemory)

    // if cross_zone_load_balancing is passed in the .delta file, use that
    val crossZoneLoadBalancing = bc.crossZoneLoadBalancing.getOrElse(true)

    val asgMinSize = config.requiredInt("aws.asg.min.size")
    val asgMaxSize = config.requiredInt("aws.asg.max.size")
    val asgDesiredSize = config.requiredInt("aws.asg.desired.size")

    DefaultSettings(
      asgHealthCheckGracePeriod = config.requiredInt("aws.asg.healthcheck.grace.period"),
      asgMinSize = asgMinSize,
      asgMaxSize = asgMaxSize,
      asgDesiredSize = asgDesiredSize,
      elbSslCertificateId = config.requiredString("aws.elb.ssl.certificate.flow"),
      apibuilderSslCertificateId = config.requiredString("aws.elb.ssl.certificate.apibuilder"),
      elbSubnets = config.requiredString("aws.elb.subnets").split(",").toIndexedSeq,
      asgSubnets = config.requiredString("aws.autoscaling.subnets").split(",").toIndexedSeq,
      lcSecurityGroup = config.requiredString("aws.launch.configuration.security.group"),
      elbSecurityGroup = config.requiredString("aws.service.security.group"),
      elbCrossZoneLoadBalancing = crossZoneLoadBalancing,
      ec2KeyName = config.requiredString("aws.service.key"),
      launchConfigImageId = latestAmi,
      launchConfigIamInstanceProfile = config.requiredString("aws.launch.configuration.role"),
      serviceRole = config.requiredString("aws.service.role"),
      instanceType = instanceType,
      jvmMemory = jvmMemory,
      containerMemory = containerMemory,
      instanceMemory = instanceMemorySettings.instance,
      ebsMemory = ebs.ebs,
      portContainer = bc.portContainer,
      portHost = bc.portHost,
      version = bc.version.getOrElse("1.0"),  // default delta version
      healthcheckUrl = bc.healthcheckUrl.getOrElse(config.requiredString("aws.elb.healthcheck.url")),
      remoteLogging = bc.remoteLogging.getOrElse(true)
    )
  }.getOrElse {
    sys.error(s"Build[$buildId] Must have build configuration before getting settings for auto scaling group")
  }

}
