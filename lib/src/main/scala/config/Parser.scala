package io.flow.delta.lib.config

import io.flow.delta.config.v0.models
import io.flow.delta.config.v0.models.{Branch, BuildConfig, BuildConfigUndefinedType, BuildStage, Cluster, Config, ConfigError, ConfigProject, EcsBuildConfig, InstanceType, K8sBuildConfig, ProjectStage}
import org.yaml.snakeyaml.Yaml

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
        Defaults.EcsConfig
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

          val builds = obj.get("builds").map(toBuild).getOrElse {
            Seq(Defaults.EcsBuildConfig)
          }

          val config = ConfigProject(
            stages = toProjectStages(
              builds = builds,
              disable = stagesMap.get("disable").map(toStringArray).getOrElse(Nil),
              enable = stagesMap.get("enable").map(toStringArray).getOrElse(Nil)
            ),
            branches = obj.get("branches").map(toStringArray(_).map(n => Branch(name = n))).getOrElse {
              Seq(Defaults.Branch)
            },
            builds = builds,
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
      case Nil => config.copy(builds = Seq(Defaults.EcsBuildConfig))
      case _ => config
    }
  }

  def toBuild(obj: Object): Seq[BuildConfig] = {
    obj match {
      case ar: java.util.ArrayList[_] => {
        ar.asScala.map {
          case name: java.lang.String => {
            Defaults.EcsBuildConfig.copy(name = name)
          }

          case map: java.util.HashMap[_, _] => {
            mapToBuild(toMap(map))
          }
        }
      }

      case _ => {
        Seq(Defaults.EcsBuildConfig)
      }
    }
  }

  private[this] def cluster(map: Map[String, String]): Cluster = {
    map.get("cluster") match {
      case None => Cluster.Ecs
      case Some(cluster) => Cluster.fromString(cluster).getOrElse {
        sys.error(s"Invalid cluster[$cluster]. Must be one of: " + Cluster.all.map(_.toString).mkString(", "))
      }
    }
  }

  def mapToBuild(data: Map[String, Any]): BuildConfig = {
    val all = data.map { case (name, attributes) =>
      val map = toMapString(attributes)
      cluster(map) match {
        case Cluster.K8s => k8sBuildConfig(name)
        case Cluster.Ecs => ecsBuildConfig(name, attributes)
        case Cluster.UNDEFINED(other) => sys.error(s"Build named '$name': Invalid cluster '$other'")
      }
    }
    all.toList match {
      case Nil => sys.error("No builds found")
      case build :: Nil => build
      case _ => sys.error("Multiple builds found")
    }
  }

  private[this] def k8sBuildConfig(name: String): K8sBuildConfig = {
    K8sBuildConfig(
      name = name,
      cluster = Cluster.K8s,
    )
  }

  private[this] def ecsBuildConfig(name: String, attributes: Any): EcsBuildConfig = {
    val map = toMapString(attributes)
    val obj = toMap(attributes)

    val instanceType = map.get("instance.type") match {
      case None => Defaults.EcsBuildConfig.instanceType
      case Some(typ) => InstanceType.fromString(typ).getOrElse {
        sys.error(s"Invalid instance type[$typ]. Must be one of: " + InstanceType.all.map(_.toString).mkString(", "))
      }
    }

    models.EcsBuildConfig(
      name = name.toString,
      cluster = Some(Cluster.Ecs),
      dockerfile = map.getOrElse("dockerfile", Defaults.EcsBuildConfig.dockerfile),
      initialNumberInstances = map.get("initial.number.instances").map(_.toLong).getOrElse(Defaults.EcsBuildConfig.initialNumberInstances),
      instanceType = instanceType,
      memory = map.get("memory").map(_.toLong),
      portContainer = map.get("port.container").map(_.toInt).getOrElse(Defaults.EcsBuildConfig.portContainer),
      portHost = map.get("port.host").map(_.toInt).getOrElse(Defaults.EcsBuildConfig.portHost),
      remoteLogging = Some(map.get("remote.logging").forall(_.toBoolean)),
      stages = toBuildStages(
        disable = obj.get("disable").map(toStringArray).getOrElse(Nil),
        enable = obj.get("enable").map(toStringArray).getOrElse(Nil)
      ),
      dependencies = obj.get("dependencies").map(toStringArray).getOrElse(Nil),
      version = map.get("version"),
      healthcheckUrl = map.get("healthcheck.url"),
      crossZoneLoadBalancing = map.get("cross.zone.load.balancing").map(_.toBoolean),
      containerMemory = map.get("container.memory").map(_.toLong),
      allowDowntime = map.get("allow.downtime").map(_.toBoolean)
    )
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

  private[this] def toProjectStages(builds: Seq[BuildConfig], disable: Seq[String], enable: Seq[String]): Seq[ProjectStage] = {
    if (areAllBuildsKubernetes(builds)) {
      Nil
    } else {
      toEcsProjectStages(disable: Seq[String], enable: Seq[String])
    }
  }

  def areAllBuildsKubernetes(builds: Seq[BuildConfig]): Boolean = {
    val all = builds.map {
      case _: K8sBuildConfig => Cluster.K8s
      case _: EcsBuildConfig | BuildConfigUndefinedType(_) => Cluster.Ecs
    }
    all.nonEmpty && all.forall(_ == Cluster.K8s)
  }

  private[this] def toEcsProjectStages(disable: Seq[String], enable: Seq[String]): Seq[ProjectStage] = {
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
