/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.4.100
 * apibuilder 0.14.3 app.apibuilder.io/flow/delta-config/0.4.100/anorm_2_6_parsers
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

  object Build {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.config.v0.models.Build] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      dockerfile: String = "dockerfile",
      initialNumberInstances: String = "initial_number_instances",
      instanceType: String = "instance_type",
      memory: String = "memory",
      portContainer: String = "port_container",
      portHost: String = "port_host",
      stages: String = "stages",
      dependencies: String = "dependencies",
      version: String = "version",
      healthcheckUrl: String = "healthcheck_url",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.config.v0.models.Build] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + dockerfile) ~
      SqlParser.long(prefixOpt.getOrElse("") + initialNumberInstances) ~
      io.flow.delta.config.v0.anorm.parsers.InstanceType.parser(prefixOpt.getOrElse("") + instanceType) ~
      SqlParser.long(prefixOpt.getOrElse("") + memory).? ~
      SqlParser.int(prefixOpt.getOrElse("") + portContainer) ~
      SqlParser.int(prefixOpt.getOrElse("") + portHost) ~
      SqlParser.get[Seq[io.flow.delta.config.v0.models.BuildStage]](prefixOpt.getOrElse("") + stages) ~
      SqlParser.get[Seq[String]](prefixOpt.getOrElse("") + dependencies) ~
      SqlParser.str(prefixOpt.getOrElse("") + version).? ~
      SqlParser.str(prefixOpt.getOrElse("") + healthcheckUrl).? map {
        case name ~ dockerfile ~ initialNumberInstances ~ instanceType ~ memory ~ portContainer ~ portHost ~ stages ~ dependencies ~ version ~ healthcheckUrl => {
          io.flow.delta.config.v0.models.Build(
            name = name,
            dockerfile = dockerfile,
            initialNumberInstances = initialNumberInstances,
            instanceType = instanceType,
            memory = memory,
            portContainer = portContainer,
            portHost = portHost,
            stages = stages,
            dependencies = dependencies,
            version = version,
            healthcheckUrl = healthcheckUrl
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
      SqlParser.get[Seq[io.flow.delta.config.v0.models.Build]](prefixOpt.getOrElse("") + builds) ~
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