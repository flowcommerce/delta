package io.flow.delta.aws

import java.util

import com.amazonaws.auth.AWSStaticCredentialsProvider
import io.flow.play.util.Config
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2ClientBuilder}
import com.amazonaws.services.autoscaling.{AmazonAutoScaling, AmazonAutoScalingClientBuilder}
import com.amazonaws.services.autoscaling.model._
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import play.api.Logger
import sun.misc.BASE64Encoder

import collection.JavaConverters._

@javax.inject.Singleton
class AutoScalingGroup @javax.inject.Inject() (
  config: Config,
  credentials: Credentials,
  configuration: Configuration
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

  lazy val encoder = new BASE64Encoder()

  /**
  * Defined Values, probably make object vals somewhere?
  */
  val launchConfigBlockDeviceMappings: util.List[BlockDeviceMapping] = Seq(
    new BlockDeviceMapping()
      .withDeviceName("/dev/xvda")
      .withEbs(new Ebs()
        .withDeleteOnTermination(true)
        .withVolumeSize(16)
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
      Logger.info(s"AWS AutoScalingGroup createLaunchConfiguration id[$id]")
      client.createLaunchConfiguration(
        new CreateLaunchConfigurationRequest()
          .withLaunchConfigurationName(name)
          .withAssociatePublicIpAddress(false)
          .withIamInstanceProfile(settings.launchConfigIamInstanceProfile)
          .withBlockDeviceMappings(launchConfigBlockDeviceMappings)
          .withSecurityGroups(Seq(settings.lcSecurityGroup).asJava)
          .withKeyName(settings.ec2KeyName)
          .withImageId(settings.launchConfigImageId)
          .withInstanceType(settings.launchConfigInstanceType)
          .withUserData(encoder.encode(lcUserData(id, settings).getBytes))
      )
    } catch {
      case e: AlreadyExistsException => println(s"Launch Configuration '$name' already exists")
    }

    name
  }

  def deleteAutoScalingGroup(id: String): String = {
    val name = getAutoScalingGroupName(id)
    Logger.info(s"AWS delete ASG projectId[$id]")

    try {
      client.deleteAutoScalingGroup(
        new DeleteAutoScalingGroupRequest()
          .withAutoScalingGroupName(name)
          .withForceDelete(true)
      )
    } catch {
      case e: Throwable => Logger.error(s"Error deleting autoscaling group $name - Error: ${e.getMessage}")
    }

    name
  }

  def deleteLaunchConfiguration(settings: Settings, id: String): String = {
    val name = getLaunchConfigurationName(settings, id)
    Logger.info(s"AWS delete launch config projectId[$id]")

    try {
      client.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(name))
    } catch {
      case e: Throwable => Logger.error(s"Error deleting launch configuration $name - Error: ${e.getMessage}")
    }

    name
  }

  def upsertAutoScalingGroup(settings: Settings, id: String, launchConfigName: String, loadBalancerName: String): String = {
    val name = getAutoScalingGroupName(id)

    client.describeAutoScalingGroups(
      new DescribeAutoScalingGroupsRequest()
        .withAutoScalingGroupNames(Seq(name).asJava)
    ).getAutoScalingGroups.asScala.headOption match {
      case None => {
        createAutoScalingGroup(settings, name, launchConfigName, loadBalancerName)
      }
      case Some(asg) => {
        if (asg.getLaunchConfigurationName != launchConfigName) {
          val instances = asg.getInstances.asScala.map(_.getInstanceId).asJava
          val oldLaunchConfigurationName = asg.getLaunchConfigurationName
          updateAutoScalingGroup(name, launchConfigName, oldLaunchConfigurationName, instances)
        }
      }
    }

    name
  }

  def createAutoScalingGroup(settings: Settings, name: String, launchConfigName: String, loadBalancerName: String) {
    try {
      Logger.info(s"AWS AutoScalingGroup createAutoScalingGroup $name")
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
    } catch {
      case e: Throwable => Logger.error(s"Error creating autoscaling group $name with launch config $launchConfigName and load balancer $loadBalancerName. Error: ${e.getMessage}")
    }
  }

  /**
    * NOTE:
    * This will update the launch configuration for the ASG. The instances
    * will need to be manually rotated for the LC to take affect. There is
    * a helper script for rotations in the infra-tools/aws repo.
    */
  def updateAutoScalingGroup(name: String, newlaunchConfigName: String, oldLaunchConfigurationName: String, instances: java.util.Collection[String]) {
    try {
      updateGroupLaunchConfiguration(name, newlaunchConfigName)
    } catch {
      case e: Throwable => Logger.error(s"FlowError Error updating autoscaling group $name with launch config $newlaunchConfigName. Error: ${e.getMessage}")
    }
  }

  private[this] def terminateInstances(instances: java.util.Collection[String]): Unit = {
    /**
      * terminate the old instances to allow new ones to come
      * up with updated launch config and load balancer
      */
    ec2Client.terminateInstances(
      new TerminateInstancesRequest()
        .withInstanceIds(instances)
    )
  }

  private[this] def deleteOldLaunchConfiguration(oldLaunchConfigurationName: String): Unit = {
    // delete the old launch configuration
    client.deleteLaunchConfiguration(
      new DeleteLaunchConfigurationRequest()
        .withLaunchConfigurationName(oldLaunchConfigurationName)
    )
  }

  private[this] def detachOldInstances(name: String, instances: java.util.Collection[String]): Unit = {
    // detach the old instances
    client.detachInstances(
      new DetachInstancesRequest()
        .withAutoScalingGroupName(name)
        .withInstanceIds(instances)
        .withShouldDecrementDesiredCapacity(false)
    )
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
      """curl -o /tmp/sumo.sh https://collectors.sumologic.com/rest/download/linux/64""",
      """chmod +x /tmp/sumo.sh""",
      """export PRIVATE_IP=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)""",
      s"""sh /tmp/sumo.sh -q -Vsumo.accessid="${sumoId}" -Vsumo.accesskey="${sumoKey}" -VsyncSources="/etc/sumo/sources.json" -Vcollector.name="${id}-""" + "$PRIVATE_IP\""
    )

    // other ECS, AWS, and OPSWORK stuff
    val completeEcsAndAwsSetup = Seq(
      s"""echo '* soft nofile $nofileMax' >> /etc/security/limits.conf""",
      s"""echo '* hard nofile $nofileMax' >> /etc/security/limits.conf""",
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
