package io.flow.delta.actors.functions

import java.util.concurrent.TimeoutException

import javax.inject.Inject
import db._
import io.flow.delta.actors.DataBuild
import io.flow.delta.api.lib.{BuildLockUtil, EventLogProcessor}
import io.flow.delta.config.v0.{models => config}
import io.flow.delta.lib.{BuildNames, DockerHost}
import io.flow.delta.v0.models.{Build, Organization, Project, Visibility, EventType => DeltaEventType}
import io.flow.log.RollbarLogger
import io.flow.util.Config
import io.flow.travis.ci.v0.Client
import io.flow.travis.ci.v0.models._
import play.api.libs.ws.WSClient

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

case class TravisCiBuild(
    version: String,
    org: Organization,
    project: Project,
    build: Build,
    buildConfig: config.Build,
    wSClient: WSClient
) {
  def withProject[T](f: Project => T): Option[T] = {
    Option(f(project))
  }

  def withBuild[T](f: Build => T): Option[T] = {
    Option(f(build))
  }
}

class TravisCiDockerImageBuilder @Inject()(
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  buildLockUtil: BuildLockUtil,
  config: Config,
  eventsDao: EventsDao,
  eventLogProcessor: EventLogProcessor,
  wsClient: WSClient,
  override val logger: RollbarLogger,
  implicit val ec: ExecutionContext
) extends DataBuild {

  def buildDockerImage(travisCiBuild: TravisCiBuild): Unit = {
    val dockerImageName = BuildNames.dockerImageName(travisCiBuild.org.docker, travisCiBuild.build, travisCiBuild.buildConfig)
    val projectId = travisCiBuild.project.id

    buildLockUtil.withLock(travisCiBuild.build.id) ({

      try {
        val client = createClient(travisCiBuild)

        val response = client.requests.get(
            repositorySlug = travisRepositorySlug(travisCiBuild),
            limit = Some(20)
        )
        val requestGetResponse = Await.result(response, 5.seconds)

        val requests = requestGetResponse.requests
          .filter(_.eventType == EventType.Api)
          .filter(_.branchName.name.getOrElse("") == travisCiBuild.version)
          .filter(_.commit.message.getOrElse("").contains(dockerImageName))

        requests match {
          case Nil => {
            // No matching builds from Travis. Check the Event log to see
            // if we tried to submit a build, otherwise submit a new build.
            eventsDao.findAll(
              projectId = Some(travisCiBuild.project.id),
              `type` = Some(DeltaEventType.Change),
              summaryKeywords = Some(travisChangedMessage(dockerImageName, travisCiBuild.version)),
              limit = Some(1)
            ).headOption match {
              case None => {
                postBuildRequest(travisCiBuild, client)
              }
              case Some(_) => {
                eventLogProcessor.checkpoint(s"Waiting for triggered build [${dockerImageName}:${travisCiBuild.version}]", log = log(projectId))
              }
            }
          }
          case requests => {
            requests.foreach { request =>
              request.builds.foreach { build =>
                eventLogProcessor.checkpoint(s"Travis CI build [${dockerImageName}:${travisCiBuild.version}], number: ${build.number}, state: ${build.state}", log = log(projectId))
              }
            }
          }
        }

      } catch {
        case _: TimeoutException => {
          eventLogProcessor.error(s"Timeout expired fetching Travis CI requests [${dockerImageName}:${travisCiBuild.version}]", log = log(projectId))
        }
        case io.flow.docker.registry.v0.errors.UnitResponse(code) => {
          eventLogProcessor.error(s"Travis CI returned HTTP $code when fetching requests [${dockerImageName}:${travisCiBuild.version}]", log = log(projectId))
        }
        case err: Throwable => {
          err.printStackTrace(System.err)
          eventLogProcessor.error(s"Error fetching Travis CI requests [${dockerImageName}:${travisCiBuild.version}]: $err", log = log(projectId))
        }
      }
    })
  }

  private def postBuildRequest(travisCiBuild: TravisCiBuild, client: Client): Unit = {
    val dockerImageName = BuildNames.dockerImageName(travisCiBuild.org.docker, travisCiBuild.build, travisCiBuild.buildConfig)
    val projectId = travisCiBuild.project.id

    try {

      val response = client.requests.post(
        repositorySlug = travisRepositorySlug(travisCiBuild),
        requestPostForm = createRequestPostForm(travisCiBuild)
      )
      Await.result(response, 5.seconds)
      eventLogProcessor.changed(travisChangedMessage(dockerImageName, travisCiBuild.version), log = log(projectId))

    } catch {
      case _: TimeoutException => {
        eventLogProcessor.error(s"Timeout expired triggering Travis CI build [${dockerImageName}:${travisCiBuild.version}]", log = log(projectId))
      }
      case io.flow.docker.registry.v0.errors.UnitResponse(code) => {
        code match {
          case _ => {
            eventLogProcessor.error(s"Travis CI returned HTTP $code when triggering build [${dockerImageName}:${travisCiBuild.version}]", log = log(projectId))
          }
        }
      }
      case err: Throwable => {
        err.printStackTrace(System.err)
        eventLogProcessor.error(s"Error triggering Travis CI build [${dockerImageName}:${travisCiBuild.version}]: $err", log = log(projectId))
      }
    }
  }

  private def createRequestPostForm(travisCiBuild: TravisCiBuild): RequestPostForm = {
    val dockerImageName = BuildNames.dockerImageName(travisCiBuild.org.docker, travisCiBuild.build, travisCiBuild.buildConfig)

    RequestPostForm(
      request = RequestPostFormData(
        branch = travisCiBuild.version,
        message = Option(travisCommitMessage(dockerImageName, travisCiBuild.version)),
        config = RequestConfigData(
          mergeMode = Option(MergeMode.Merge),
          branches = Option(RequestConfigBranchesData(
            only = Option(Seq("/^\\d+\\.\\d+\\.\\d+$/"))
          )),
          dist = Option("trusty"),
          env = Option(Seq("DELTA=skipping-env-setting")),
          sudo = Option("required"),
          services = Option(Seq("docker")),
          addons = Option(RequestConfigAddonsData(
            apt = Option(RequestConfigAddonsAptData(
              packages = Option(Seq("docker-ce=17.05.0~ce-0~ubuntu-trusty"))
            ))
          )),
          beforeInstall = Option(Seq("echo Delta: skipping before_install step")),
          install = Option(Seq("echo Delta: skipping install step")),
          beforeScript = Option(Seq("echo Delta: skipping before_script step")),
          script = Option(
            script(travisCiBuild, dockerImageName)
          ),
          jobs = None,
          stages = None,
          afterScript = Option(Seq("echo Delta: skipping after_script step")),
          afterSuccess = Option(Seq("echo Delta: skipping after_success step")),
          afterFailure = Option(Seq("echo Delta: skipping after_failure step")),
          beforeDeploy = Option(Seq("echo Delta: skipping before_deploy step")),
          deploy = None,
          afterDeploy = Option(Seq("echo Delta: skipping after_deploy step"))
        )
      )
    )
  }

  private def createRequestHeaders(travisCiBuild: TravisCiBuild): Seq[(String, String)] = {
    val token = if (travisCiBuild.project.visibility == Visibility.Public) {
      config.requiredString("travis.delta.auth.token.public")
    } else {
      config.requiredString("travis.delta.auth.token.private")
    }

    Seq(
      ("Travis-API-Version", "3"),
      ("Authorization", s"token ${token}")
    )
  }

  private def createClient(travisCiBuild: TravisCiBuild): Client = {
    // Travis separates public and private projects into separate domains
    val baseUrl = if (travisCiBuild.project.visibility == Visibility.Public) {
      "https://api.travis-ci.org"
    } else {
      "https://api.travis-ci.com"
    }

    new Client(wsClient, baseUrl, None, createRequestHeaders(travisCiBuild))
  }

  private def travisRepositorySlug(travisCiBuild: TravisCiBuild): String = {
    travisCiBuild.org.travis.organization + "/" + travisCiBuild.project.id
  }

  private def travisChangedMessage(dockerImageName: String, version: String): String = {
    s"Triggered docker build for ${dockerImageName}:${version}"
  }

  private def travisCommitMessage(dockerImageName: String, version: String): String = {
    s"Delta: building image ${dockerImageName}:${version}"
  }

  private def script(build: TravisCiBuild, dockerImageName: String): Seq[String] = {
    DockerHost(build.buildConfig) match {
      case DockerHost.Ecr => ecrScript(build, dockerImageName)
      case DockerHost.DockerHub => dockerHubScript(build, dockerImageName)
    }
  }

  private[this] def ecrScript(travisCiBuild: TravisCiBuild, dockerImageName: String): Seq[String] = {
    Seq(
      "docker --version",
      "echo TRAVIS_BRANCH=$TRAVIS_BRANCH",
      "pip install --user awscli",
      "export PATH=$PATH:/$HOME/.local/bin",
      "eval $(aws ecr get-login --no-include-email --region us-east-1)",
      s"docker build --build-arg NPM_TOKEN=$${NPM_TOKEN} --build-arg STRIPE_PUBLISHABLE_KEY=$${STRIPE_PUBLISHABLE_KEY} --build-arg AWS_ACCESS_KEY_ID=$${AWS_ACCESS_KEY_ID} --build-arg AWS_SECRET_ACCESS_KEY=$${AWS_SECRET_ACCESS_KEY} --build-arg GOOGLE_PLACES_API_KEY=$${GOOGLE_PLACES_API_KEY} --build-arg NATERO_API_KEY=$${NATERO_API_KEY} --build-arg NATERO_AUTH_KEY=$${NATERO_AUTH_KEY} -f ${travisCiBuild.buildConfig.dockerfile} -t ${dockerImageName}:$${TRAVIS_BRANCH} .",
      s"docker push ${dockerImageName}:$${TRAVIS_BRANCH}"
    )
  }

  private[this] def dockerHubScript(travisCiBuild: TravisCiBuild, dockerImageName: String): Seq[String] = {
    Seq(
      "docker --version",
      "echo TRAVIS_BRANCH=$TRAVIS_BRANCH",
      s"docker build --build-arg NPM_TOKEN=$${NPM_TOKEN} --build-arg STRIPE_PUBLISHABLE_KEY=$${STRIPE_PUBLISHABLE_KEY} --build-arg AWS_ACCESS_KEY_ID=$${AWS_ACCESS_KEY_ID} --build-arg AWS_SECRET_ACCESS_KEY=$${AWS_SECRET_ACCESS_KEY} --build-arg GOOGLE_PLACES_API_KEY=$${GOOGLE_PLACES_API_KEY} --build-arg NATERO_API_KEY=$${NATERO_API_KEY} --build-arg NATERO_AUTH_KEY=$${NATERO_AUTH_KEY} -f ${travisCiBuild.buildConfig.dockerfile} -t ${dockerImageName}:$${TRAVIS_BRANCH} .",
      "docker login -u=$DOCKER_USERNAME -p=$DOCKER_PASSWORD",
      s"docker push ${dockerImageName}:$${TRAVIS_BRANCH}"
    )
  }
}
