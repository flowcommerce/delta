package io.flow.delta.lib.config

import io.flow.delta.config.v0.models.{Branch, Build, BuildStage, ConfigError, ConfigProject, ConfigUndefinedType, InstanceType, ProjectStage}
import java.io.File
import org.specs2.mutable._

class ParserSpec extends Specification {

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
    configProject(read("empty.txt")) must beEqualTo(Defaults.Config)

    configProject(read("location.txt")) must beEqualTo(
      Defaults.Config.copy(
        builds = Seq(
          Defaults.Build.copy(
            portContainer = 9000,
            portHost = 6191
          )
        )
      )
    )

    configProject(read("delta.txt")) must beEqualTo(
      Defaults.Config.copy(
        builds = Seq(
          Defaults.Build.copy(
            name = "api",
            dockerfile = "api/Dockerfile",
            portContainer = 9000,
            portHost = 6091,
            initialNumberInstances = 1
          ),
          Defaults.Build.copy(
            name = "www",
            dockerfile = "www/Dockerfile",
            portContainer = 9000,
            portHost = 6090
          )
        )
      )
    )

    configProject(read("complete.txt")) must beEqualTo(
      Defaults.Config.copy(
        branches = Seq(Branch(name = "master"), Branch(name = "release")),
        stages = Seq(ProjectStage.SyncShas, ProjectStage.SyncTags),
        builds = Seq(
          Defaults.Build.copy(name = "api"),
          Build(
            name = "www",
            dockerfile = "www/Dockerfile",
            instanceType = InstanceType.T2Medium,
            memory = 8150,
            initialNumberInstances = 10,
            portContainer = 7050,
            portHost = 8000,
            stages = Seq(BuildStage.SetDesiredState, BuildStage.BuildDockerImage),
            dependencies = Seq("api")
          )
        )
      )
    )
  }

  "Sample configuration files can all parse" in {
    for ( file <- ConfigSampleDir.listFiles if file.getName.endsWith(".txt") ) {
      parser.parse(read(file)) match {
        case c: ConfigProject => {}
        case c: ConfigError => sys.error(s"Failed to parse file[$file]: ${c.errors}")
        case ConfigUndefinedType(other) => sys.error(s"Invalid project config[$other]")
      }
    }
    true must beTrue
  }

  "Empty file" in {
    configProject("") must be(Defaults.Config)
    configProject("   ") must be(Defaults.Config)
  }

  "Invalid file" in {
    parser.parse("!@#$lkasdu4d") match {
      case ConfigError(errors) => errors.isEmpty must beFalse
      case _ => sys.error("No error when parsing invalid yaml")
    }
  }

  "project stages" in {
    configProject("").stages must be(ProjectStage.all)

    configProject("""
stages:
  enable:
    - tag
    """).stages must beEqualTo(Seq(ProjectStage.Tag))

    configProject("""
stages:
  disable:
    - tag
    """).stages must beEqualTo(Seq(ProjectStage.SyncShas, ProjectStage.SyncTags))
  }

  "Branches" in {
    configProject("""
branches:
  - master
    """) must beEqualTo(Defaults.Config)

    configProject("""
branches:
  - master
  - release
    """).branches.map(_.name) must beEqualTo(Seq("master", "release"))
  }

  "Builds" in {
    configProject("""
builds:
  - root
    """) must beEqualTo(Defaults.Config)

    configProject("""
builds:
  - api
  - www
    """).builds.toList match {
      case api :: www :: Nil => {
        api must beEqualTo(Defaults.Build.copy(name = "api"))
        www must beEqualTo(Defaults.Build.copy(name = "www"))
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
        api must beEqualTo(
          Defaults.Build.copy(
            name = "api",
            dockerfile = "api/Dockerfile",
            initialNumberInstances = 5,
            instanceType = InstanceType.T2Medium,
            memory = 3500,
            stages = Seq(BuildStage.SetDesiredState, BuildStage.SyncDockerImage, BuildStage.BuildDockerImage)
          )
        )
        www must beEqualTo(
          Defaults.Build.copy(
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
        build must beEqualTo(
          Defaults.Build.copy(instanceType = InstanceType.T2Medium, memory = 1000)
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
        build must beEqualTo(
          Defaults.Build.copy(portContainer = 9000, portHost = 6021)
        )
      }

      case _ => sys.error("Expected two branches")
    }
  }

}
