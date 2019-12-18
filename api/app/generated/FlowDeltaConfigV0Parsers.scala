/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.8.16
 * apibuilder 0.14.89 app.apibuilder.io/flow/delta-config/latest/anorm_2_6_parsers
 */
import anorm._

package io.flow.delta.config.v0.anorm.parsers {

  import io.flow.delta.config.v0.anorm.conversions.Standard._

  import io.flow.delta.config.v0.anorm.conversions.Types._

  object BuildStage {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.BuildStage] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "build_stage", prefixOpt: Option[String] = None): RowParser[io.flow.delta.config.v0.models.BuildStage] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.config.v0.models.BuildStage(value)
      }
    }

  }

  object Cluster {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.Cluster] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "cluster", prefixOpt: Option[String] = None): RowParser[io.flow.delta.config.v0.models.Cluster] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.config.v0.models.Cluster(value)
      }
    }

  }

  object InstanceType {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.InstanceType] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "instance_type", prefixOpt: Option[String] = None): RowParser[io.flow.delta.config.v0.models.InstanceType] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.config.v0.models.InstanceType(value)
      }
    }

  }

  object ProjectStage {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.ProjectStage] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "project_stage", prefixOpt: Option[String] = None): RowParser[io.flow.delta.config.v0.models.ProjectStage] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.config.v0.models.ProjectStage(value)
      }
    }

  }

  object Branch {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.Branch] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.config.v0.models.Branch] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case name => {
          io.flow.delta.config.v0.models.Branch(
            name = name
          )
        }
      }
    }

  }

  object ConfigError {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.ConfigError] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      errors: String = "errors",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.config.v0.models.ConfigError] = {
      SqlParser.get[Seq[String]](prefixOpt.getOrElse("") + errors) map {
        case errors => {
          io.flow.delta.config.v0.models.ConfigError(
            errors = errors
          )
        }
      }
    }

  }

  object ConfigProject {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.ConfigProject] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      stages: String = "stages",
      builds: String = "builds",
      branches: String = "branches",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.config.v0.models.ConfigProject] = {
      SqlParser.get[Seq[io.flow.delta.config.v0.models.ProjectStage]](prefixOpt.getOrElse("") + stages) ~
      SqlParser.get[Seq[io.flow.delta.config.v0.models.BuildConfig]](prefixOpt.getOrElse("") + builds) ~
      SqlParser.get[Seq[io.flow.delta.config.v0.models.Branch]](prefixOpt.getOrElse("") + branches) map {
        case stages ~ builds ~ branches => {
          io.flow.delta.config.v0.models.ConfigProject(
            stages = stages,
            builds = builds,
            branches = branches
          )
        }
      }
    }

  }

  object EcsBuildConfig {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.EcsBuildConfig] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      cluster: String = "cluster",
      dockerfile: String = "dockerfile",
      initialNumberInstances: String = "initial_number_instances",
      instanceType: String = "instance_type",
      memory: String = "memory",
      containerMemory: String = "container_memory",
      portContainer: String = "port_container",
      portHost: String = "port_host",
      remoteLogging: String = "remote_logging",
      stages: String = "stages",
      dependencies: String = "dependencies",
      version: String = "version",
      healthcheckUrl: String = "healthcheck_url",
      crossZoneLoadBalancing: String = "cross_zone_load_balancing",
      allowDowntime: String = "allow_downtime",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.config.v0.models.EcsBuildConfig] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.delta.config.v0.anorm.parsers.Cluster.parser(prefixOpt.getOrElse("") + cluster).? ~
      SqlParser.str(prefixOpt.getOrElse("") + dockerfile) ~
      SqlParser.long(prefixOpt.getOrElse("") + initialNumberInstances) ~
      io.flow.delta.config.v0.anorm.parsers.InstanceType.parser(prefixOpt.getOrElse("") + instanceType) ~
      SqlParser.long(prefixOpt.getOrElse("") + memory).? ~
      SqlParser.long(prefixOpt.getOrElse("") + containerMemory).? ~
      SqlParser.int(prefixOpt.getOrElse("") + portContainer) ~
      SqlParser.int(prefixOpt.getOrElse("") + portHost) ~
      SqlParser.bool(prefixOpt.getOrElse("") + remoteLogging).? ~
      SqlParser.get[Seq[io.flow.delta.config.v0.models.BuildStage]](prefixOpt.getOrElse("") + stages) ~
      SqlParser.get[Seq[String]](prefixOpt.getOrElse("") + dependencies) ~
      SqlParser.str(prefixOpt.getOrElse("") + version).? ~
      SqlParser.str(prefixOpt.getOrElse("") + healthcheckUrl).? ~
      SqlParser.bool(prefixOpt.getOrElse("") + crossZoneLoadBalancing).? ~
      SqlParser.bool(prefixOpt.getOrElse("") + allowDowntime).? map {
        case name ~ cluster ~ dockerfile ~ initialNumberInstances ~ instanceType ~ memory ~ containerMemory ~ portContainer ~ portHost ~ remoteLogging ~ stages ~ dependencies ~ version ~ healthcheckUrl ~ crossZoneLoadBalancing ~ allowDowntime => {
          io.flow.delta.config.v0.models.EcsBuildConfig(
            name = name,
            cluster = cluster,
            dockerfile = dockerfile,
            initialNumberInstances = initialNumberInstances,
            instanceType = instanceType,
            memory = memory,
            containerMemory = containerMemory,
            portContainer = portContainer,
            portHost = portHost,
            remoteLogging = remoteLogging,
            stages = stages,
            dependencies = dependencies,
            version = version,
            healthcheckUrl = healthcheckUrl,
            crossZoneLoadBalancing = crossZoneLoadBalancing,
            allowDowntime = allowDowntime
          )
        }
      }
    }

  }

  object K8sBuildConfig {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.K8sBuildConfig] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      cluster: String = "cluster",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.config.v0.models.K8sBuildConfig] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.delta.config.v0.anorm.parsers.Cluster.parser(prefixOpt.getOrElse("") + cluster) map {
        case name ~ cluster => {
          io.flow.delta.config.v0.models.K8sBuildConfig(
            name = name,
            cluster = cluster
          )
        }
      }
    }

  }

  object BuildConfig {

    def parserWithPrefix(prefix: String, sep: String = "_") = {
      io.flow.delta.config.v0.anorm.parsers.EcsBuildConfig.parser(prefixOpt = Some(s"$prefix$sep")) |
      io.flow.delta.config.v0.anorm.parsers.K8sBuildConfig.parser(prefixOpt = Some(s"$prefix$sep"))
    }

    def parser() = {
      io.flow.delta.config.v0.anorm.parsers.EcsBuildConfig.parser() |
      io.flow.delta.config.v0.anorm.parsers.K8sBuildConfig.parser()
    }

  }

  object Config {

    def parserWithPrefix(prefix: String, sep: String = "_") = {
      io.flow.delta.config.v0.anorm.parsers.ConfigProject.parser(prefixOpt = Some(s"$prefix$sep")) |
      io.flow.delta.config.v0.anorm.parsers.ConfigError.parser(prefixOpt = Some(s"$prefix$sep"))
    }

    def parser() = {
      io.flow.delta.config.v0.anorm.parsers.ConfigProject.parser() |
      io.flow.delta.config.v0.anorm.parsers.ConfigError.parser()
    }

  }

}