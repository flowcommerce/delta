package io.flow.delta.lib.config

import java.io.File

import io.flow.delta.config.v0.models._
import org.scalatestplus.play.PlaySpec

class ParserSpec extends PlaySpec {

  private[this] lazy val parser = new Parser()
  private[this] val ConfigSampleDir = new File("lib/src/test/resources/config")

  def configProject(value: String): ConfigProject = {
    parser.parse(value) match {
      case c: ConfigProject => c
      case c: ConfigError => sys.error(s"Failed to parse config[${c.errors}]\n$value")
      case ConfigUndefinedType(other) => sys.error(s"Invalid project config[$other]")
    }
  }

  def read(name: String): String = {
    read(new File(ConfigSampleDir, name))
  }

  def read(path: File): String = {
    scala.io.Source.fromFile(path).getLines.toSeq.mkString("\n")
  }

  "Samples" in {
    configProject(read("empty.txt")) must be(Defaults.EcsConfig)

    configProject(read("location.txt")) must be(
      Defaults.EcsConfig.copy(
        builds = Seq(
          Defaults.EcsBuild.copy(
            portContainer = 9000,
            portHost = 6191
          )
        )
      )
    )

    configProject(read("delta.txt")) must be(
      Defaults.EcsConfig.copy(
        builds = Seq(
          Defaults.EcsBuild.copy(
            name = "api",
            dockerfile = "api/Dockerfile",
            portContainer = 9000,
            portHost = 6091,
            initialNumberInstances = 1,
            remoteLogging = Some(true)
          ),
          Defaults.EcsBuild.copy(
            name = "www",
            dockerfile = "www/Dockerfile",
            portContainer = 9000,
            portHost = 6090,
            remoteLogging = Some(false)
          )
        )
      )
    )

    configProject(read("k8s.txt")) must be(Defaults.K8sConfig)

    configProject(read("complete.txt")) must be(
      Defaults.EcsConfig.copy(
        branches = Seq(Branch(name = "master"), Branch(name = "release")),
        stages = Seq(ProjectStage.SyncShas, ProjectStage.SyncTags),
        builds = Seq(
          Defaults.EcsBuild.copy(name = "api"),
          EcsBuildConfig(
            name = "www",
            cluster = Some(Cluster.Ecs),
            dockerfile = "www/Dockerfile",
            instanceType = InstanceType.T2Medium,
            memory = Some(8150),
            initialNumberInstances = 10,
            portContainer = 7050,
            portHost = 8000,
            stages = BuildStage.all.filter { _ != BuildStage.Scale },
            dependencies = Seq("api"),
            remoteLogging = Some(true), // defaults to true
            allowDowntime = None
          )
        )
      )
    )
  }

  "Sample configuration files can all parse" in {
    for ( file <- ConfigSampleDir.listFiles if file.getName.endsWith(".txt") ) {
      parser.parse(read(file)) match {
        case _: ConfigProject => {}
        case c: ConfigError => sys.error(s"Failed to parse file[$file]: ${c.errors}")
        case ConfigUndefinedType(other) => sys.error(s"Invalid project config[$other]")
      }
    }
    true must be(true)
  }

  "Empty file" in {
    configProject("") must be(Defaults.EcsConfig)
    configProject("   ") must be(Defaults.EcsConfig)
  }

  "Invalid file" in {
    parser.parse("!@#$lkasdu4d") match {
      case ConfigError(errors) => errors.isEmpty must be(false)
      case _ => sys.error("No error when parsing invalid yaml")
    }
  }

  "project stages" in {
    configProject("").stages must be(ProjectStage.all)

    configProject("""
stages:
  enable:
    - tag
    """).stages must be(Seq(ProjectStage.Tag))

    configProject("""
stages:
  disable:
    - tag
    """).stages must be(Seq(ProjectStage.SyncShas, ProjectStage.SyncTags))
  }

  "Branches" in {
    configProject("""
branches:
  - master
    """) must be(Defaults.EcsConfig)

    configProject("""
branches:
  - master
  - release
    """).branches.map(_.name) must be(Seq("master", "release"))
  }

  "Builds" in {
    configProject("""
builds:
  - root
    """) must be(Defaults.EcsConfig)

    configProject("""
builds:
  - api
  - www
    """).builds.toList match {
      case api :: www :: Nil => {
        api must be(Defaults.EcsBuild.copy(name = "api"))
        www must be(Defaults.EcsBuild.copy(name = "www"))
      }

      case _ => sys.error("Expected two branches")
    }

    configProject("""
builds:
  - api:
      dockerfile: api/Dockerfile
      instance.type: t2.medium
      initial.number.instances: 5
      disable:
        - scale
  - www:
      dockerfile: www/Dockerfile
      initial.number.instances: 10
      enable:
        - set_desired_state
        - sync_docker_image
        - build_docker_image
      dependencies:
        - api
    """).builds.toList match {
      case api :: www :: Nil => {
        api must be(
          Defaults.EcsBuild.copy(
            name = "api",
            dockerfile = "api/Dockerfile",
            initialNumberInstances = 5,
            instanceType = InstanceType.T2Medium,
            memory = None,
            stages = Seq(BuildStage.SetDesiredState, BuildStage.SyncDockerImage, BuildStage.BuildDockerImage)
          )
        )
        www must be(
          Defaults.EcsBuild.copy(
            name = "www",
            dockerfile = "www/Dockerfile",
            initialNumberInstances = 10,
            stages = Seq(BuildStage.SetDesiredState, BuildStage.SyncDockerImage, BuildStage.BuildDockerImage),
            dependencies = Seq("api")
          )
        )
      }

      case _ => sys.error("Expected two branches")
    }

    configProject("""
builds:
  - root:
      instance.type: t2.medium
      memory: 1000
    """).builds.toList match {
      case build :: Nil => {
        build must be(
          Defaults.EcsBuild.copy(instanceType = InstanceType.T2Medium, memory = Some(1000))
        )
      }

      case _ => sys.error("Expected two branches")
    }

    configProject("""
builds:
  - root:
      port.container: 9000
      port.host: 6021
    """).builds.toList match {
      case build :: Nil => {
        build must be(
          Defaults.EcsBuild.copy(portContainer = 9000, portHost = 6021)
        )
      }

      case _ => sys.error("Expected two branches")
    }
  }

  "areAllBuildsKubernetes" in {
    parser.areAllBuildsKubernetes(Nil) must be(false)
    parser.areAllBuildsKubernetes(Seq(Defaults.K8sBuild)) must be(true)
    parser.areAllBuildsKubernetes(Seq(Defaults.EcsBuild)) must be(false)
    parser.areAllBuildsKubernetes(Seq(Defaults.EcsBuild, Defaults.K8sBuild)) must be(false)
    parser.areAllBuildsKubernetes(Seq(Defaults.K8sBuild, Defaults.K8sBuild)) must be(true)
  }
}
