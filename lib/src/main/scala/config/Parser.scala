package io.flow.delta.lib.config

import io.flow.delta.config.v0.models.{Branch, Build, BuildStage, Config, ConfigError, ConfigProject, InstanceType, ProjectStage}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.scanner.ScannerException

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Parses the contents of the .delta file
  */
case class Parser() {

  def parse(
    contents: String
  ): Config = {
    contents.trim match {
      case "" => {
        Defaults.Config
      }

      case value => {
        val yaml = new Yaml()

        Try {
          val y = Option(yaml.loadAs(value, classOf[java.util.Map[String, Object]]))

          val obj = y match {
            case None => Map[String, Object]()
            case Some(data) => data.asInstanceOf[java.util.Map[String, Object]].asScala
          }
          val stagesMap: Map[String, Object] = obj.get("stages") match {
            case None => Map[String, Object]()
            case Some(data) => data.asInstanceOf[java.util.Map[String, Object]].asScala.toMap
          }

          val config = ConfigProject(
            stages = toProjectStages(
              disable = stagesMap.get("disable").map(toStringArray(_)).getOrElse(Nil),
              enable = stagesMap.get("enable").map(toStringArray(_)).getOrElse(Nil)
            ),
            branches = obj.get("branches").map(toStringArray(_).map(n => Branch(name = n))).getOrElse {
              Seq(Defaults.Branch)
            },
            builds = obj.get("builds").map(toBuild(_)).getOrElse {
              Seq(Defaults.Build)
            }
          )
          ensureBuild(ensureBranch(config))
        } match {
          case Success(config) => {
            config
          }

          case Failure(ex) => {
            ConfigError(
              errors = Seq(ex.getMessage)
            )
          }
        }
      }
    }
  }

  private[this] def ensureBranch(config: ConfigProject): ConfigProject = {
    config.branches.toList match {
      case Nil => config.copy(branches = Seq(Defaults.Branch))
      case _ => config
    }
  }

  private[this] def ensureBuild(config: ConfigProject): ConfigProject = {
    config.builds.toList match {
      case Nil => config.copy(builds = Seq(Defaults.Build))
      case _ => config
    }
  }

  def toBuild(obj: Object): Seq[Build] = {
    obj match {
      case ar: java.util.ArrayList[_] => {
        ar.asScala.map { data =>
          data match {
            case name: java.lang.String => {
              Defaults.Build.copy(name = name)
            }

            case map: java.util.HashMap[_, _] => {
              mapToBuild(toMap(map))
            }
          }
        }
      }

      case _ => {
        Seq(Defaults.Build)
      }
    }
  }

  def mapToBuild(data: Map[String, Any]): Build = {
    val all = data.map { case (name, attributes) =>
      val map = toMapString(attributes)
      val obj = toMap(attributes)
      val instanceType = map.get("instance.type") match {
        case None => Defaults.Build.instanceType
        case Some(typ) => InstanceType.fromString(typ).getOrElse {
          sys.error(s"Invalid instance type[$typ]. Must be one of: " + InstanceType.all.map(_.toString).mkString(", "))
        }
      }

      Build(
        name = name.toString,
        dockerfile = map.get("dockerfile").getOrElse(Defaults.Build.dockerfile),
        initialNumberInstances = map.get("initial.number.instances").map(_.toLong).getOrElse(Defaults.Build.initialNumberInstances),
        instanceType = instanceType,
        memory = map.get("memory").map(_.toLong),
        portContainer = map.get("port.container").map(_.toInt).getOrElse(Defaults.Build.portContainer),
        portHost = map.get("port.host").map(_.toInt).getOrElse(Defaults.Build.portHost),
        remoteLogging = Some(map.get("remote.logging").map(_.toBoolean).getOrElse(true)),
        stages = toBuildStages(
          disable = obj.get("disable").map(toStringArray(_)).getOrElse(Nil),
          enable = obj.get("enable").map(toStringArray(_)).getOrElse(Nil)
        ),
        dependencies = obj.get("dependencies").map(toStringArray(_)).getOrElse(Nil),
        version = map.get("version"),
        healthcheckUrl = map.get("healthcheck.url"),
        crossZoneLoadBalancing = map.get("cross.zone.load.balancing").map(_.toBoolean)
      )
    }

    all.toList match {
      case Nil => sys.error("No builds found")
      case build :: Nil => build
      case _ => sys.error("Multiple builds found")
    }
  }

  private[this] def toBuildStages(disable: Seq[String], enable: Seq[String]): Seq[BuildStage] = {
    (disable.isEmpty, enable.isEmpty) match {
      case (true, true) => {
        BuildStage.all
      }

      case (true, false) => {
        BuildStage.all.filter(stage => enable.contains(stage.toString))
      }

      case (false, true) => {
        BuildStage.all.filter(stage => !disable.contains(stage.toString))
      }

      case (false, false) => {
        BuildStage.all.filter { stage =>
          enable.contains(stage.toString) && !disable.contains(stage.toString)
        }
      }
    }
  }

  private[this] def toProjectStages(disable: Seq[String], enable: Seq[String]): Seq[ProjectStage] = {
    (disable.isEmpty, enable.isEmpty) match {
      case (true, true) => {
        ProjectStage.all
      }

      case (true, false) => {
        ProjectStage.all.filter(stage => enable.contains(stage.toString))
      }

      case (false, true) => {
        ProjectStage.all.filter(stage => !disable.contains(stage.toString))
      }

      case (false, false) => {
        ProjectStage.all.filter { stage =>
          enable.contains(stage.toString) && !disable.contains(stage.toString)
        }
      }
    }
  }

  private[this] def toStringArray(obj: Any): Seq[String] = {
    obj match {
      case v: java.lang.String => Seq(v)
      case ar: java.util.ArrayList[_] => ar.asScala.map(_.toString)
      case _ => Nil
    }
  }

  private[this] def toMapString(value: Any): Map[String, String] = {
    toMap(value).map { case (key, value) => (key -> value.toString) }
  }

  private[this] def toMap(value: Any): Map[String, Any] = {
    value match {
      case map: java.util.HashMap[_, _] => {
        map.asScala.map { case (key, value) =>
          (key.toString -> value)
        }.toMap
      }

      case _ => {
        Map.empty
      }
    }
  }
}
