/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.5.57
 * apibuilder 0.14.3 app.apibuilder.io/flow/delta/0.5.57/anorm_2_6_parsers
 */
import anorm._

package io.flow.delta.v0.anorm.parsers {

  import io.flow.delta.v0.anorm.conversions.Standard._

  import io.flow.common.v0.anorm.conversions.Types._
  import io.flow.delta.config.v0.anorm.conversions.Types._
  import io.flow.delta.v0.anorm.conversions.Types._

  object DockerProvider {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.DockerProvider] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "docker_provider", prefixOpt: Option[String] = None): RowParser[io.flow.delta.v0.models.DockerProvider] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.v0.models.DockerProvider(value)
      }
    }

  }

  object EventType {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.EventType] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "event_type", prefixOpt: Option[String] = None): RowParser[io.flow.delta.v0.models.EventType] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.v0.models.EventType(value)
      }
    }

  }

  object Publication {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Publication] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "publication", prefixOpt: Option[String] = None): RowParser[io.flow.delta.v0.models.Publication] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.v0.models.Publication(value)
      }
    }

  }

  object Role {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Role] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "role", prefixOpt: Option[String] = None): RowParser[io.flow.delta.v0.models.Role] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.v0.models.Role(value)
      }
    }

  }

  object Scms {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Scms] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "scms", prefixOpt: Option[String] = None): RowParser[io.flow.delta.v0.models.Scms] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.v0.models.Scms(value)
      }
    }

  }

  object Status {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Status] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "status", prefixOpt: Option[String] = None): RowParser[io.flow.delta.v0.models.Status] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.v0.models.Status(value)
      }
    }

  }

  object Visibility {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Visibility] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "visibility", prefixOpt: Option[String] = None): RowParser[io.flow.delta.v0.models.Visibility] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.flow.delta.v0.models.Visibility(value)
      }
    }

  }

  object AwsActor {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.AwsActor] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.AwsActor] = {
      SqlParser.long(prefixOpt.getOrElse("") + id) map {
        case id => {
          io.flow.delta.v0.models.AwsActor(
            id = id
          )
        }
      }
    }

  }

  object Build {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Build] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      projectPrefix: String = "project",
      status: String = "status",
      name: String = "name",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Build] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.ProjectSummary.parserWithPrefix(prefixOpt.getOrElse("") + projectPrefix) ~
      io.flow.delta.v0.anorm.parsers.Status.parser(prefixOpt.getOrElse("") + status) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case id ~ project ~ status ~ name => {
          io.flow.delta.v0.models.Build(
            id = id,
            project = project,
            status = status,
            name = name
          )
        }
      }
    }

  }

  object BuildState {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.BuildState] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      desiredPrefix: String = "desired",
      lastPrefix: String = "last",
      latestImage: String = "latest_image",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.BuildState] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.delta.v0.anorm.parsers.State.parserWithPrefix(prefixOpt.getOrElse("") + desiredPrefix).? ~
      io.flow.delta.v0.anorm.parsers.State.parserWithPrefix(prefixOpt.getOrElse("") + lastPrefix).? ~
      SqlParser.str(prefixOpt.getOrElse("") + latestImage).? map {
        case name ~ desired ~ last ~ latestImage => {
          io.flow.delta.v0.models.BuildState(
            name = name,
            desired = desired,
            last = last,
            latestImage = latestImage
          )
        }
      }
    }

  }

  object DashboardBuild {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.DashboardBuild] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      projectPrefix: String = "project",
      name: String = "name",
      desiredPrefix: String = "desired",
      lastPrefix: String = "last",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.DashboardBuild] = {
      io.flow.delta.v0.anorm.parsers.ProjectSummary.parserWithPrefix(prefixOpt.getOrElse("") + projectPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.delta.v0.anorm.parsers.State.parserWithPrefix(prefixOpt.getOrElse("") + desiredPrefix) ~
      io.flow.delta.v0.anorm.parsers.State.parserWithPrefix(prefixOpt.getOrElse("") + lastPrefix) map {
        case project ~ name ~ desired ~ last => {
          io.flow.delta.v0.models.DashboardBuild(
            project = project,
            name = name,
            desired = desired,
            last = last
          )
        }
      }
    }

  }

  object Docker {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Docker] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      provider: String = "provider",
      organization: String = "organization",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Docker] = {
      io.flow.delta.v0.anorm.parsers.DockerProvider.parser(prefixOpt.getOrElse("") + provider) ~
      SqlParser.str(prefixOpt.getOrElse("") + organization) map {
        case provider ~ organization => {
          io.flow.delta.v0.models.Docker(
            provider = provider,
            organization = organization
          )
        }
      }
    }

  }

  object Event {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Event] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      createdAt: String = "created_at",
      projectPrefix: String = "project",
      `type`: String = "type",
      summary: String = "summary",
      error: String = "error",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Event] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + createdAt) ~
      io.flow.delta.v0.anorm.parsers.ProjectSummary.parserWithPrefix(prefixOpt.getOrElse("") + projectPrefix) ~
      io.flow.delta.v0.anorm.parsers.EventType.parser(prefixOpt.getOrElse("") + `type`) ~
      SqlParser.str(prefixOpt.getOrElse("") + summary) ~
      SqlParser.str(prefixOpt.getOrElse("") + error).? map {
        case id ~ createdAt ~ project ~ typeInstance ~ summary ~ error => {
          io.flow.delta.v0.models.Event(
            id = id,
            createdAt = createdAt,
            project = project,
            `type` = typeInstance,
            summary = summary,
            error = error
          )
        }
      }
    }

  }

  object GithubAuthenticationForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.GithubAuthenticationForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      code: String = "code",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.GithubAuthenticationForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + code) map {
        case code => {
          io.flow.delta.v0.models.GithubAuthenticationForm(
            code = code
          )
        }
      }
    }

  }

  object GithubUser {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.GithubUser] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      userPrefix: String = "user",
      githubUserId: String = "github_user_id",
      login: String = "login",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.GithubUser] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.Reference.parserWithPrefix(prefixOpt.getOrElse("") + userPrefix) ~
      SqlParser.long(prefixOpt.getOrElse("") + githubUserId) ~
      SqlParser.str(prefixOpt.getOrElse("") + login) map {
        case id ~ user ~ githubUserId ~ login => {
          io.flow.delta.v0.models.GithubUser(
            id = id,
            user = user,
            githubUserId = githubUserId,
            login = login
          )
        }
      }
    }

  }

  object GithubUserForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.GithubUserForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      userId: String = "user_id",
      githubUserId: String = "github_user_id",
      login: String = "login",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.GithubUserForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + userId) ~
      SqlParser.long(prefixOpt.getOrElse("") + githubUserId) ~
      SqlParser.str(prefixOpt.getOrElse("") + login) map {
        case userId ~ githubUserId ~ login => {
          io.flow.delta.v0.models.GithubUserForm(
            userId = userId,
            githubUserId = githubUserId,
            login = login
          )
        }
      }
    }

  }

  object GithubWebhook {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.GithubWebhook] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.GithubWebhook] = {
      SqlParser.long(prefixOpt.getOrElse("") + id) map {
        case id => {
          io.flow.delta.v0.models.GithubWebhook(
            id = id
          )
        }
      }
    }

  }

  object Image {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Image] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      buildPrefix: String = "build",
      name: String = "name",
      version: String = "version",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Image] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.Build.parserWithPrefix(prefixOpt.getOrElse("") + buildPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + version) map {
        case id ~ build ~ name ~ version => {
          io.flow.delta.v0.models.Image(
            id = id,
            build = build,
            name = name,
            version = version
          )
        }
      }
    }

  }

  object ImageForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.ImageForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      buildId: String = "build_id",
      name: String = "name",
      version: String = "version",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.ImageForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + buildId) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + version) map {
        case buildId ~ name ~ version => {
          io.flow.delta.v0.models.ImageForm(
            buildId = buildId,
            name = name,
            version = version
          )
        }
      }
    }

  }

  object Item {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Item] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      organizationPrefix: String = "organization",
      visibility: String = "visibility",
      summaryPrefix: String = "summary",
      label: String = "label",
      description: String = "description",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Item] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.OrganizationSummary.parserWithPrefix(prefixOpt.getOrElse("") + organizationPrefix) ~
      io.flow.delta.v0.anorm.parsers.Visibility.parser(prefixOpt.getOrElse("") + visibility) ~
      io.flow.delta.v0.anorm.parsers.ItemSummary.parserWithPrefix(prefixOpt.getOrElse("") + summaryPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + label) ~
      SqlParser.str(prefixOpt.getOrElse("") + description).? map {
        case id ~ organization ~ visibility ~ summary ~ label ~ description => {
          io.flow.delta.v0.models.Item(
            id = id,
            organization = organization,
            visibility = visibility,
            summary = summary,
            label = label,
            description = description
          )
        }
      }
    }

  }

  object Membership {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Membership] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      userPrefix: String = "user",
      organizationPrefix: String = "organization",
      role: String = "role",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Membership] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.UserSummary.parserWithPrefix(prefixOpt.getOrElse("") + userPrefix) ~
      io.flow.delta.v0.anorm.parsers.OrganizationSummary.parserWithPrefix(prefixOpt.getOrElse("") + organizationPrefix) ~
      io.flow.delta.v0.anorm.parsers.Role.parser(prefixOpt.getOrElse("") + role) map {
        case id ~ user ~ organization ~ role => {
          io.flow.delta.v0.models.Membership(
            id = id,
            user = user,
            organization = organization,
            role = role
          )
        }
      }
    }

  }

  object MembershipForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.MembershipForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      userId: String = "user_id",
      organization: String = "organization",
      role: String = "role",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.MembershipForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + userId) ~
      SqlParser.str(prefixOpt.getOrElse("") + organization) ~
      io.flow.delta.v0.anorm.parsers.Role.parser(prefixOpt.getOrElse("") + role) map {
        case userId ~ organization ~ role => {
          io.flow.delta.v0.models.MembershipForm(
            userId = userId,
            organization = organization,
            role = role
          )
        }
      }
    }

  }

  object Organization {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Organization] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      userPrefix: String = "user",
      dockerPrefix: String = "docker",
      travisPrefix: String = "travis",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Organization] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.UserSummary.parserWithPrefix(prefixOpt.getOrElse("") + userPrefix) ~
      io.flow.delta.v0.anorm.parsers.Docker.parserWithPrefix(prefixOpt.getOrElse("") + dockerPrefix) ~
      io.flow.delta.v0.anorm.parsers.Travis.parserWithPrefix(prefixOpt.getOrElse("") + travisPrefix) map {
        case id ~ user ~ docker ~ travis => {
          io.flow.delta.v0.models.Organization(
            id = id,
            user = user,
            docker = docker,
            travis = travis
          )
        }
      }
    }

  }

  object OrganizationForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.OrganizationForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      dockerPrefix: String = "docker",
      travisPrefix: String = "travis",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.OrganizationForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.Docker.parserWithPrefix(prefixOpt.getOrElse("") + dockerPrefix) ~
      io.flow.delta.v0.anorm.parsers.Travis.parserWithPrefix(prefixOpt.getOrElse("") + travisPrefix) map {
        case id ~ docker ~ travis => {
          io.flow.delta.v0.models.OrganizationForm(
            id = id,
            docker = docker,
            travis = travis
          )
        }
      }
    }

  }

  object OrganizationSummary {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.OrganizationSummary] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.OrganizationSummary] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) map {
        case id => {
          io.flow.delta.v0.models.OrganizationSummary(
            id = id
          )
        }
      }
    }

  }

  object Project {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Project] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      organizationPrefix: String = "organization",
      userPrefix: String = "user",
      visibility: String = "visibility",
      scms: String = "scms",
      name: String = "name",
      uri: String = "uri",
      configPrefix: String = "config",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Project] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.OrganizationSummary.parserWithPrefix(prefixOpt.getOrElse("") + organizationPrefix) ~
      io.flow.delta.v0.anorm.parsers.Reference.parserWithPrefix(prefixOpt.getOrElse("") + userPrefix) ~
      io.flow.delta.v0.anorm.parsers.Visibility.parser(prefixOpt.getOrElse("") + visibility) ~
      io.flow.delta.v0.anorm.parsers.Scms.parser(prefixOpt.getOrElse("") + scms) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + uri) ~
      io.flow.delta.config.v0.anorm.parsers.Config.parserWithPrefix(prefixOpt.getOrElse("") + configPrefix) map {
        case id ~ organization ~ user ~ visibility ~ scms ~ name ~ uri ~ config => {
          io.flow.delta.v0.models.Project(
            id = id,
            organization = organization,
            user = user,
            visibility = visibility,
            scms = scms,
            name = name,
            uri = uri,
            config = config
          )
        }
      }
    }

  }

  object ProjectForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.ProjectForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      organization: String = "organization",
      name: String = "name",
      visibility: String = "visibility",
      scms: String = "scms",
      uri: String = "uri",
      configPrefix: String = "config",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.ProjectForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + organization) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.delta.v0.anorm.parsers.Visibility.parser(prefixOpt.getOrElse("") + visibility) ~
      io.flow.delta.v0.anorm.parsers.Scms.parser(prefixOpt.getOrElse("") + scms) ~
      SqlParser.str(prefixOpt.getOrElse("") + uri) ~
      io.flow.delta.config.v0.anorm.parsers.ConfigProject.parserWithPrefix(prefixOpt.getOrElse("") + configPrefix).? map {
        case organization ~ name ~ visibility ~ scms ~ uri ~ config => {
          io.flow.delta.v0.models.ProjectForm(
            organization = organization,
            name = name,
            visibility = visibility,
            scms = scms,
            uri = uri,
            config = config
          )
        }
      }
    }

  }

  object ProjectSummary {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.ProjectSummary] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      organizationPrefix: String = "organization",
      name: String = "name",
      uri: String = "uri",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.ProjectSummary] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.OrganizationSummary.parserWithPrefix(prefixOpt.getOrElse("") + organizationPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + uri) map {
        case id ~ organization ~ name ~ uri => {
          io.flow.delta.v0.models.ProjectSummary(
            id = id,
            organization = organization,
            name = name,
            uri = uri
          )
        }
      }
    }

  }

  object Reference {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Reference] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Reference] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) map {
        case id => {
          io.flow.delta.v0.models.Reference(
            id = id
          )
        }
      }
    }

  }

  object Repository {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Repository] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      visibility: String = "visibility",
      uri: String = "uri",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Repository] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      io.flow.delta.v0.anorm.parsers.Visibility.parser(prefixOpt.getOrElse("") + visibility) ~
      SqlParser.str(prefixOpt.getOrElse("") + uri) map {
        case name ~ visibility ~ uri => {
          io.flow.delta.v0.models.Repository(
            name = name,
            visibility = visibility,
            uri = uri
          )
        }
      }
    }

  }

  object Sha {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Sha] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      projectPrefix: String = "project",
      createdAt: String = "created_at",
      branch: String = "branch",
      hash: String = "hash",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Sha] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.ProjectSummary.parserWithPrefix(prefixOpt.getOrElse("") + projectPrefix) ~
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + createdAt) ~
      SqlParser.str(prefixOpt.getOrElse("") + branch) ~
      SqlParser.str(prefixOpt.getOrElse("") + hash) map {
        case id ~ project ~ createdAt ~ branch ~ hash => {
          io.flow.delta.v0.models.Sha(
            id = id,
            project = project,
            createdAt = createdAt,
            branch = branch,
            hash = hash
          )
        }
      }
    }

  }

  object State {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.State] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      timestamp: String = "timestamp",
      versions: String = "versions",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.State] = {
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + timestamp) ~
      SqlParser.get[Seq[io.flow.delta.v0.models.Version]](prefixOpt.getOrElse("") + versions) map {
        case timestamp ~ versions => {
          io.flow.delta.v0.models.State(
            timestamp = timestamp,
            versions = versions
          )
        }
      }
    }

  }

  object StateForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.StateForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      versions: String = "versions",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.StateForm] = {
      SqlParser.get[Seq[io.flow.delta.v0.models.Version]](prefixOpt.getOrElse("") + versions) map {
        case versions => {
          io.flow.delta.v0.models.StateForm(
            versions = versions
          )
        }
      }
    }

  }

  object Subscription {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Subscription] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      userPrefix: String = "user",
      publication: String = "publication",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Subscription] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.Reference.parserWithPrefix(prefixOpt.getOrElse("") + userPrefix) ~
      io.flow.delta.v0.anorm.parsers.Publication.parser(prefixOpt.getOrElse("") + publication) map {
        case id ~ user ~ publication => {
          io.flow.delta.v0.models.Subscription(
            id = id,
            user = user,
            publication = publication
          )
        }
      }
    }

  }

  object SubscriptionForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.SubscriptionForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      userId: String = "user_id",
      publication: String = "publication",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.SubscriptionForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + userId) ~
      io.flow.delta.v0.anorm.parsers.Publication.parser(prefixOpt.getOrElse("") + publication) map {
        case userId ~ publication => {
          io.flow.delta.v0.models.SubscriptionForm(
            userId = userId,
            publication = publication
          )
        }
      }
    }

  }

  object Tag {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Tag] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      projectPrefix: String = "project",
      createdAt: String = "created_at",
      name: String = "name",
      hash: String = "hash",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Tag] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.ProjectSummary.parserWithPrefix(prefixOpt.getOrElse("") + projectPrefix) ~
      SqlParser.get[_root_.org.joda.time.DateTime](prefixOpt.getOrElse("") + createdAt) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + hash) map {
        case id ~ project ~ createdAt ~ name ~ hash => {
          io.flow.delta.v0.models.Tag(
            id = id,
            project = project,
            createdAt = createdAt,
            name = name,
            hash = hash
          )
        }
      }
    }

  }

  object Token {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Token] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      userPrefix: String = "user",
      masked: String = "masked",
      cleartext: String = "cleartext",
      description: String = "description",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Token] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.Reference.parserWithPrefix(prefixOpt.getOrElse("") + userPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + masked) ~
      SqlParser.str(prefixOpt.getOrElse("") + cleartext).? ~
      SqlParser.str(prefixOpt.getOrElse("") + description).? map {
        case id ~ user ~ masked ~ cleartext ~ description => {
          io.flow.delta.v0.models.Token(
            id = id,
            user = user,
            masked = masked,
            cleartext = cleartext,
            description = description
          )
        }
      }
    }

  }

  object TokenForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.TokenForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      userId: String = "user_id",
      description: String = "description",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.TokenForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + userId) ~
      SqlParser.str(prefixOpt.getOrElse("") + description).? map {
        case userId ~ description => {
          io.flow.delta.v0.models.TokenForm(
            userId = userId,
            description = description
          )
        }
      }
    }

  }

  object Travis {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Travis] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      organization: String = "organization",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Travis] = {
      SqlParser.str(prefixOpt.getOrElse("") + organization) map {
        case organization => {
          io.flow.delta.v0.models.Travis(
            organization = organization
          )
        }
      }
    }

  }

  object UserForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.UserForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      email: String = "email",
      namePrefix: String = "name",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.UserForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + email).? ~
      io.flow.common.v0.anorm.parsers.Name.parserWithPrefix(prefixOpt.getOrElse("") + namePrefix).? map {
        case email ~ name => {
          io.flow.delta.v0.models.UserForm(
            email = email,
            name = name
          )
        }
      }
    }

  }

  object UserIdentifier {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.UserIdentifier] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      userPrefix: String = "user",
      value: String = "value",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.UserIdentifier] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.Reference.parserWithPrefix(prefixOpt.getOrElse("") + userPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + value) map {
        case id ~ user ~ value => {
          io.flow.delta.v0.models.UserIdentifier(
            id = id,
            user = user,
            value = value
          )
        }
      }
    }

  }

  object UserSummary {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.UserSummary] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      email: String = "email",
      namePrefix: String = "name",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.UserSummary] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      SqlParser.str(prefixOpt.getOrElse("") + email).? ~
      io.flow.common.v0.anorm.parsers.Name.parserWithPrefix(prefixOpt.getOrElse("") + namePrefix) map {
        case id ~ email ~ name => {
          io.flow.delta.v0.models.UserSummary(
            id = id,
            email = email,
            name = name
          )
        }
      }
    }

  }

  object UsernamePassword {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.UsernamePassword] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      username: String = "username",
      password: String = "password",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.UsernamePassword] = {
      SqlParser.str(prefixOpt.getOrElse("") + username) ~
      SqlParser.str(prefixOpt.getOrElse("") + password).? map {
        case username ~ password => {
          io.flow.delta.v0.models.UsernamePassword(
            username = username,
            password = password
          )
        }
      }
    }

  }

  object Variable {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Variable] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      id: String = "id",
      organizationPrefix: String = "organization",
      key: String = "key",
      value: String = "value",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Variable] = {
      SqlParser.str(prefixOpt.getOrElse("") + id) ~
      io.flow.delta.v0.anorm.parsers.OrganizationSummary.parserWithPrefix(prefixOpt.getOrElse("") + organizationPrefix) ~
      SqlParser.str(prefixOpt.getOrElse("") + key) ~
      SqlParser.str(prefixOpt.getOrElse("") + value) map {
        case id ~ organization ~ key ~ value => {
          io.flow.delta.v0.models.Variable(
            id = id,
            organization = organization,
            key = key,
            value = value
          )
        }
      }
    }

  }

  object VariableForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.VariableForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      organization: String = "organization",
      key: String = "key",
      value: String = "value",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.VariableForm] = {
      SqlParser.str(prefixOpt.getOrElse("") + organization) ~
      SqlParser.str(prefixOpt.getOrElse("") + key) ~
      SqlParser.str(prefixOpt.getOrElse("") + value) map {
        case organization ~ key ~ value => {
          io.flow.delta.v0.models.VariableForm(
            organization = organization,
            key = key,
            value = value
          )
        }
      }
    }

  }

  object Version {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.flow.delta.v0.models.Version] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      instances: String = "instances",
      prefixOpt: Option[String] = None
    ): RowParser[io.flow.delta.v0.models.Version] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.long(prefixOpt.getOrElse("") + instances) map {
        case name ~ instances => {
          io.flow.delta.v0.models.Version(
            name = name,
            instances = instances
          )
        }
      }
    }

  }

  object ItemSummary {

    def parserWithPrefix(prefix: String, sep: String = "_") = {
      io.flow.delta.v0.anorm.parsers.ProjectSummary.parser(prefixOpt = Some(s"$prefix$sep"))
    }

    def parser() = {
      io.flow.delta.v0.anorm.parsers.ProjectSummary.parser()
    }

  }

}