package io.flow.delta.aws

import io.flow.delta.config.v0.models.InstanceType

case class DefaultSettings(
  instanceType: InstanceType,
  override val asgHealthCheckGracePeriod: Int,
  override val asgMinSize: Int,
  override val asgMaxSize: Int,
  override val asgDesiredSize: Int,
  override val elbSslCertificateId: String,
  override val apibuilderSslCertificateId: String,
  override val elbSubnets: Seq[String],
  override val asgSubnets: Seq[String],
  override val lcSecurityGroup: String,
  override val elbSecurityGroup: String,
  override val ec2KeyName: String,
  override val launchConfigImageId: String,
  override val launchConfigIamInstanceProfile: String,
  override val serviceRole: String,
  override val containerMemory: Int, // in MB
  override val portContainer: Int,
  override val portHost: Int,
  override val version: String
) extends Settings {

  override val launchConfigInstanceType = instanceType.toString
}

/**
* Single place to find all the AWS-related configuration settings.
* Make it easy to change (or fix) in one place.
**/
trait Settings {
  /**
    * These settings come from application/base.conf files
    */
  // Length of time in seconds after a new Amazon EC2 instance comes into service that Auto Scaling starts checking its health
  val asgHealthCheckGracePeriod: Int

  // The minimum size of the Auto Scaling group
  val asgMinSize: Int

  // The maximum size of the Auto Scaling group
  val asgMaxSize: Int

  // The number of Amazon EC2 instances that should be running in the autoscaling group
  val asgDesiredSize: Int

  // SSL settings for ELB listeners
  val elbSslCertificateId: String
  val apibuilderSslCertificateId: String

  // Subnets for the load balancer and autoscaling group instances
  val elbSubnets: Seq[String]
  val asgSubnets: Seq[String]

  // Security groups for the EC2 instances launch configuration and autoscaling group
  val lcSecurityGroup: String
  val elbSecurityGroup: String

  // Keypair name used to SSH into EC2 instances created by the autoscaling group
  val ec2KeyName: String

  // Launch configuration image ID. AMI instance is: Amazon Linux AMI for ECS (2016.03)
  val launchConfigImageId: String

  // Role for the new launch configuration
  val launchConfigIamInstanceProfile: String

  // EC2 service role
  val serviceRole: String

  /**
    * These settings come from .delta configuration files
    */
  // Launch configuration EC2 instance type, ex. t2.micro
  val launchConfigInstanceType: String

  // MB of memory
  val containerMemory: Int

  val portContainer: Int

  val portHost: Int
  
  val version: String
}
