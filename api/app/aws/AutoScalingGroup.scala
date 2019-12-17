package io.flow.delta.aws

import java.util

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2ClientBuilder}
import com.amazonaws.services.autoscaling.{AmazonAutoScaling, AmazonAutoScalingClientBuilder}
import com.amazonaws.services.autoscaling.model._
import io.flow.log.RollbarLogger
import io.flow.util.Config
import org.apache.commons.codec.binary.Base64

import collection.JavaConverters._
import scala.util.control.NonFatal

@javax.inject.Singleton
class AutoScalingGroup @javax.inject.Inject() (
  config: Config,
  credentials: Credentials,
  configuration: Configuration,
  logger: RollbarLogger
) {

  private[this] lazy val dockerHubToken = config.requiredString("dockerhub.delta.auth.token")
  private[this] lazy val dockerHubEmail = config.requiredString("dockerhub.delta.auth.email")

  private[this] lazy val sumoId = config.requiredString("sumo.service.id")
  private[this] lazy val sumoKey = config.requiredString("sumo.service.key")

  private[this] lazy val awsOpsworksStackId = config.requiredString("aws.opsworks.stack.id")
  private[this] lazy val awsOpsworksLayerId = config.requiredString("aws.opsworks.layer.id")
  private[this] lazy val awsOpsworksSnsTopicArn = config.requiredString("aws.opsworks.sns.topic.arn")

  lazy val ec2Client: AmazonEC2 = AmazonEC2ClientBuilder.
    standard().
    withCredentials(new AWSStaticCredentialsProvider(credentials.aws)).
    withClientConfiguration(configuration.aws).
    build()

  lazy val client: AmazonAutoScaling = AmazonAutoScalingClientBuilder.
    standard().
    withCredentials(new AWSStaticCredentialsProvider(credentials.aws)).
    withClientConfiguration(configuration.aws).
    build()

  /**
  * Defined Values, probably make object vals somewhere?
  */
  def launchConfigBlockDeviceMappings(settings: Settings): util.List[BlockDeviceMapping] = Seq(
    new BlockDeviceMapping()
      .withDeviceName("/dev/xvda")
      .withEbs(new Ebs()
        .withDeleteOnTermination(true)
        .withVolumeSize(math.ceil(settings.ebsMemory / 1000D).toInt)
        .withVolumeType("gp2")
      ),
    new BlockDeviceMapping()
      .withDeviceName("/dev/xvdcz")
      .withEbs(new Ebs()
        .withDeleteOnTermination(true)
        .withEncrypted(false)
        .withVolumeSize(22)
        .withVolumeType("gp2")
      )
  ).asJava

  def getLaunchConfigurationName(settings: Settings, id: String) =
    s"${id.replaceAll("_", "-")}-ecs-lc-${settings.launchConfigImageId}-${settings.launchConfigInstanceType}-${settings.version}"

  def getAutoScalingGroupName(id: String) = s"${id.replaceAll("_", "-")}-ecs-auto-scaling-group"

  def createLaunchConfiguration(settings: Settings, id: String): String = {
    val name = getLaunchConfigurationName(settings, id)

    try {
      logger.fingerprint("AutoScalingGroup").withKeyValue("id", id).info(s"AWS AutoScalingGroup createLaunchConfiguration")
      client.createLaunchConfiguration(
        new CreateLaunchConfigurationRequest()
          .withLaunchConfigurationName(name)
          .withAssociatePublicIpAddress(false)
          .withIamInstanceProfile(settings.launchConfigIamInstanceProfile)
          .withBlockDeviceMappings(launchConfigBlockDeviceMappings(settings))
          .withSecurityGroups(Seq(settings.lcSecurityGroup).asJava)
          .withKeyName(settings.ec2KeyName)
          .withImageId(settings.launchConfigImageId)
          .withInstanceType(settings.launchConfigInstanceType)
          .withUserData(
            new String(Base64.encodeBase64(lcUserData(id, settings).getBytes))
          )
      )
    } catch {
      case _: AlreadyExistsException => println(s"Launch Configuration '$name' already exists")
    }

    name
  }

  def delete(id: String): String = {
    val name = getAutoScalingGroupName(id)
    logger.fingerprint("AutoScalingGroup").withKeyValue("id", id).info(s"AWS delete ASG")

    try {
      client.deleteAutoScalingGroup(
        new DeleteAutoScalingGroupRequest()
          .withAutoScalingGroupName(name)
          .withForceDelete(true)
      )
    } catch {
      case e: Throwable => logger.fingerprint("AutoScalingGroup").withKeyValue("id", id).withKeyValue("name", name).error(s"Error deleting autoscaling group", e)
    }

    name
  }

  def deleteLaunchConfiguration(settings: Settings, id: String): String = {
    val name = getLaunchConfigurationName(settings, id)
    logger.fingerprint("AutoScalingGroup").withKeyValue("id", id).withKeyValue("name", name).info(s"AWS delete launch config")

    try {
      client.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(name))
    } catch {
      case e: Throwable => logger.fingerprint("AutoScalingGroup").withKeyValue("id", id).withKeyValue("name", name).error(s"Error deleting launch configuration", e)
    }

    name
  }

  def upsert(settings: Settings, id: String, launchConfigName: String, loadBalancerName: String): String = {
    val name = getAutoScalingGroupName(id)

    client.describeAutoScalingGroups(
      new DescribeAutoScalingGroupsRequest()
        .withAutoScalingGroupNames(Seq(name).asJava)
    ).getAutoScalingGroups.asScala.headOption match {
      case None => {
        create(settings, name, launchConfigName, loadBalancerName)
      }
      case Some(asg) => {
        if (asg.getLaunchConfigurationName != launchConfigName) {
          val oldLaunchConfigurationName = asg.getLaunchConfigurationName
          update(name, launchConfigName, oldLaunchConfigurationName)
        }
      }
    }

    name
  }

  def create(settings: Settings, name: String, launchConfigName: String, loadBalancerName: String): Unit = {
    try {
      logger.fingerprint("AutoScalingGroup").withKeyValue("launchConfigName", launchConfigName).withKeyValue("name", name).info(s"AWS AutoScalingGroup createAutoScalingGroup")

      client.createAutoScalingGroup(
        new CreateAutoScalingGroupRequest()
          .withAutoScalingGroupName(name)
          .withLaunchConfigurationName(launchConfigName)
          .withLoadBalancerNames(Seq(loadBalancerName).asJava)
          .withVPCZoneIdentifier(settings.asgSubnets.mkString(","))
          .withNewInstancesProtectedFromScaleIn(false)
          .withHealthCheckType("EC2")
          .withHealthCheckGracePeriod(settings.asgHealthCheckGracePeriod)
          .withMinSize(settings.asgMinSize)
          .withMaxSize(settings.asgMaxSize)
          .withDesiredCapacity(settings.asgDesiredSize)
      )
      // Add an opsworks_stack_id tag to the instance which is used by a lambda
      // function attached to SNS to deregister from Opsworks.
      client.createOrUpdateTags(
        new CreateOrUpdateTagsRequest()
          .withTags(
            new Tag()
              .withResourceId(name)
              .withResourceType("auto-scaling-group")
              .withKey("opsworks_stack_id")
              .withValue(awsOpsworksStackId)
              .withPropagateAtLaunch(true)
          )
      )
      client.putNotificationConfiguration(
        new PutNotificationConfigurationRequest()
          .withAutoScalingGroupName(name)
          .withTopicARN(awsOpsworksSnsTopicArn)
          .withNotificationTypes(Seq("autoscaling:EC2_INSTANCE_TERMINATE").asJava)
      )
      ()
    } catch {
      case e: Throwable => logger.fingerprint("AutoScalingGroup").withKeyValue("launchConfigName", launchConfigName).withKeyValue("name", name).error(s"Error creating autoscaling group", e)
    }
  }

  /**
    * NOTE:
    * This will update the launch configuration for the ASG. The instances
    * will need to be manually rotated for the LC to take affect. There is
    * a helper script for rotations in the infra-tools/aws repo.
    */
  def update(name: String, newlaunchConfigName: String, oldLaunchConfigurationName: String): Unit = {
    try {
      updateGroupLaunchConfiguration(name, newlaunchConfigName)
    } catch {
      case NonFatal(e) => logger.fingerprint("AutoScalingGroup").withKeyValue("newlaunchConfigName", newlaunchConfigName).withKeyValue("oldLaunchConfigurationName", oldLaunchConfigurationName).withKeyValue("name", name).error("FlowError Error updating autoscaling group", e)
    }
  }

  private[this] def updateGroupLaunchConfiguration(name: String, newlaunchConfigName: String): Unit = {
    // update the auto scaling group
    client.updateAutoScalingGroup(
      new UpdateAutoScalingGroupRequest()
        .withAutoScalingGroupName(name)
        .withLaunchConfigurationName(newlaunchConfigName)
    )
    // Add an opsworks_stack_id tag to the instance which is used by a lambda
    // function attached to SNS to deregister from Opsworks.
    client.createOrUpdateTags(
      new CreateOrUpdateTagsRequest()
        .withTags(
          new Tag()
            .withResourceId(name)
            .withResourceType("auto-scaling-group")
            .withKey("opsworks_stack_id")
            .withValue(awsOpsworksStackId)
            .withPropagateAtLaunch(true)
        )
    )
    client.putNotificationConfiguration(
      new PutNotificationConfigurationRequest()
        .withAutoScalingGroupName(name)
        .withTopicARN(awsOpsworksSnsTopicArn)
        .withNotificationTypes(Seq("autoscaling:EC2_INSTANCE_TERMINATE").asJava)
    )
    ()
  }

  def lcUserData(id: String, settings: Settings): String = {
    val ecsClusterName = EC2ContainerService.getClusterName(id)
    val nofileMax = 1000000

    // register this instance with the correct cluster and other ECS stuff
    val ecsClusterRegistration = Seq(
      """#!/bin/bash""",
      s"""echo 'ECS_CLUSTER=${ecsClusterName}' >> /etc/ecs/ecs.config""",
      """echo 'ECS_ENGINE_AUTH_TYPE=dockercfg' >> /etc/ecs/ecs.config""",
      """echo 'ECS_LOGLEVEL=warn' >> /etc/ecs/ecs.config""",
      s"""echo 'ECS_ENGINE_AUTH_DATA={"https://index.docker.io/v1/":{"auth":"${dockerHubToken}","email":"${dockerHubEmail}"}}' >> /etc/ecs/ecs.config"""
    )

    // Sumo collector setup on a new ec2 instance
    val setupSumoCollector = Seq(
      """mkdir -p /etc/sumo""",
      s"""echo '{"api.version":"v1","sources":[{"sourceType":"DockerLog","name":"ecs_docker_logs","category":"${id}_docker_logs","uri":"unix:///var/run/docker.sock","allContainers":true,"collectEvents":false}]}' > /etc/sumo/sources.json""",
      """curl -o /tmp/sumo.sh https://collectors.sumologic.com/rest/download/linux/64?version=19.245-6""",
      """chmod +x /tmp/sumo.sh""",
      """export PRIVATE_IP=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)""",
      s"""sh /tmp/sumo.sh -q -Vsumo.accessid="${sumoId}" -Vsumo.accesskey="${sumoKey}" -VsyncSources="/etc/sumo/sources.json" -Vcollector.name="${id}-""" + "$PRIVATE_IP\""
    )

    // other ECS, AWS, and OPSWORK stuff
    val completeEcsAndAwsSetup = Seq(
      s"""echo '* soft nofile $nofileMax' >> /etc/security/limits.conf""",
      s"""echo '* hard nofile $nofileMax' >> /etc/security/limits.conf""",
      """echo '{"log-driver":"json-file","log-opts":{"max-size":"10m","max-file":"10"}}' > /etc/docker/daemon.json""",
      """service docker restart""",
      """sed -i'' -e 's/.*requiretty.*//' /etc/sudoers""",
      """curl -o /tmp/get-pip.py https://bootstrap.pypa.io/get-pip.py""",
      """python /tmp/get-pip.py && /bin/rm /tmp/get-pip.py""",
      """/usr/local/bin/pip install --upgrade awscli""",
      """INSTANCE_ID=$(/usr/local/bin/aws opsworks register --use-instance-profile --infrastructure-class ec2 --region us-east-1 --stack-id """ + awsOpsworksStackId + """ --override-hostname """ + id + """-$(tr -cd 'a-z' < /dev/urandom |head -c8) --local 2>&1 |grep -o 'Instance ID: .*' |cut -d' ' -f3)""",
      """/usr/local/bin/aws opsworks wait instance-registered --region us-east-1 --instance-id $INSTANCE_ID""",
      """/usr/local/bin/aws opsworks assign-instance --region us-east-1 --instance-id $INSTANCE_ID --layer-ids """ + awsOpsworksLayerId
    )

    val allSteps = if (settings.remoteLogging) {
      ecsClusterRegistration ++ setupSumoCollector ++ completeEcsAndAwsSetup
    } else {
      ecsClusterRegistration ++ completeEcsAndAwsSetup
    }

    allSteps.mkString("\n")
  }
}
