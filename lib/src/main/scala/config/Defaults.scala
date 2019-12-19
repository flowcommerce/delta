package io.flow.delta.lib.config

import io.flow.delta.lib.BuildNames
import io.flow.delta.config.v0.models
import io.flow.delta.config.v0.models.{Cluster, InstanceType}

object Defaults {

  val Branch = models.Branch(
    name = "master"
  )

  val K8sBuildConfig: models.K8sBuildConfig = models.K8sBuildConfig(
    name = BuildNames.DefaultBuildName,
    cluster = Cluster.K8s,
  )

  val EcsBuildConfig: models.EcsBuildConfig = models.EcsBuildConfig(
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
    builds = Seq(EcsBuildConfig)
  )

  val K8sConfig = models.ConfigProject(
    stages = Seq(models.ProjectStage.SyncShas, models.ProjectStage.SyncTags),
    branches = Seq(Branch),
    builds = Seq(K8sBuildConfig)
  )

}
