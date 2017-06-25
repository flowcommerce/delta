/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.3.90
 * apibuilder:0.12.3 https://api.apibuilder.io/flow/delta-config/0.3.90/anorm_2_x_parsers
 */
import anorm._

package io.flow.delta.config.v0.anorm.parsers {

  import io.flow.delta.config.v0.anorm.conversions.Standard._

  import io.flow.delta.config.v0.anorm.conversions.Types._

  object BuildStage {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(s"$prefix${sep}name")

    def parser(name: String = "build_stage"): RowParser[io.flow.delta.config.v0.models.BuildStage] = {
      SqlParser.str(name) map {
        case value => io.flow.delta.config.v0.models.BuildStage(value)
      }
    }

  }

  object InstanceType {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(s"$prefix${sep}name")

    def parser(name: String = "instance_type"): RowParser[io.flow.delta.config.v0.models.InstanceType] = {
      SqlParser.str(name) map {
        case value => io.flow.delta.config.v0.models.InstanceType(value)
      }
    }

  }

  object ProjectStage {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(s"$prefix${sep}name")

    def parser(name: String = "project_stage"): RowParser[io.flow.delta.config.v0.models.ProjectStage] = {
      SqlParser.str(name) map {
        case value => io.flow.delta.config.v0.models.ProjectStage(value)
      }
    }

  }

  object Branch {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      name = s"$prefix${sep}name"
    )

    def parser(
      name: String = "name"
    ): RowParser[io.flow.delta.config.v0.models.Branch] = {
      SqlParser.str(name) map {
        case name => {
          io.flow.delta.config.v0.models.Branch(
            name = name
          )
        }
      }
    }

  }

  object Build {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      name = s"$prefix${sep}name",
      dockerfile = s"$prefix${sep}dockerfile",
      initialNumberInstances = s"$prefix${sep}initial_number_instances",
      instanceType = s"$prefix${sep}instance_type",
      memory = s"$prefix${sep}memory",
      portContainer = s"$prefix${sep}port_container",
      portHost = s"$prefix${sep}port_host",
      stages = s"$prefix${sep}stages",
      dependencies = s"$prefix${sep}dependencies",
      version = s"$prefix${sep}version"
    )

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
      version: String = "version"
    ): RowParser[io.flow.delta.config.v0.models.Build] = {
      SqlParser.str(name) ~
      SqlParser.str(dockerfile) ~
      SqlParser.long(initialNumberInstances) ~
      io.flow.delta.config.v0.anorm.parsers.InstanceType.parser(instanceType) ~
      SqlParser.long(memory) ~
      SqlParser.int(portContainer) ~
      SqlParser.int(portHost) ~
      SqlParser.get[Seq[io.flow.delta.config.v0.models.BuildStage]](stages) ~
      SqlParser.get[Seq[String]](dependencies) ~
      SqlParser.str(version).? map {
        case name ~ dockerfile ~ initialNumberInstances ~ instanceType ~ memory ~ portContainer ~ portHost ~ stages ~ dependencies ~ version => {
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
            version = version
          )
        }
      }
    }

  }

  object ConfigError {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      errors = s"$prefix${sep}errors"
    )

    def parser(
      errors: String = "errors"
    ): RowParser[io.flow.delta.config.v0.models.ConfigError] = {
      SqlParser.get[Seq[String]](errors) map {
        case errors => {
          io.flow.delta.config.v0.models.ConfigError(
            errors = errors
          )
        }
      }
    }

  }

  object ConfigProject {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      stages = s"$prefix${sep}stages",
      builds = s"$prefix${sep}builds",
      branches = s"$prefix${sep}branches"
    )

    def parser(
      stages: String = "stages",
      builds: String = "builds",
      branches: String = "branches"
    ): RowParser[io.flow.delta.config.v0.models.ConfigProject] = {
      SqlParser.get[Seq[io.flow.delta.config.v0.models.ProjectStage]](stages) ~
      SqlParser.get[Seq[io.flow.delta.config.v0.models.Build]](builds) ~
      SqlParser.get[Seq[io.flow.delta.config.v0.models.Branch]](branches) map {
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
      io.flow.delta.config.v0.anorm.parsers.ConfigProject.parserWithPrefix(prefix, sep) |
      io.flow.delta.config.v0.anorm.parsers.ConfigError.parserWithPrefix(prefix, sep)
    }

    def parser() = {
      io.flow.delta.config.v0.anorm.parsers.ConfigProject.parser() |
      io.flow.delta.config.v0.anorm.parsers.ConfigError.parser()
    }

  }

}