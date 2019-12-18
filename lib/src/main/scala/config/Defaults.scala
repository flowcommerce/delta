package io.flow.delta.lib.config

import io.flow.delta.lib.BuildNames
import io.flow.delta.config.v0.models
import io.flow.delta.config.v0.models.{Cluster, InstanceType}

object Defaults {

  val Branch = models.Branch(
    name = "master"
  )

  val K8sBuild = models.K8sBuildConfig(
    name = BuildNames.DefaultBuildName,
    cluster = Cluster.K8s,
  )

  val EcsBuild = models.EcsBuildConfig(
    name = BuildNames.DefaultBuildName,
    cluster = Some(Cluster.Ecs),
    dockerfile = "./Dockerfile",
    initialNumberInstances = 2,
    instanceType = InstanceType.T2Micro,
    memory = None,
    portContainer = 80,
    portHost = 80,
    remoteLogging = Some(true),
    stages = models.BuildStage.all,
    dependencies = Nil,
    version = None,
    healthcheckUrl = None,
    allowDowntime = None
  )

  val EcsConfig = models.ConfigProject(
    stages = models.ProjectStage.all,
    branches = Seq(Branch),
    builds = Seq(EcsBuild)
  )

  val K8sConfig = models.ConfigProject(
    stages = Nil,
    branches = Seq(Branch),
    builds = Seq(K8sBuild)
  )

}
