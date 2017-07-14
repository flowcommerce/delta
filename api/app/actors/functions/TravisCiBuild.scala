package io.flow.delta.actors.functions

import db.EventsDao
import io.flow.delta.actors.BuildEventLog
import io.flow.delta.config.v0.models.{Build => BuildConfig}
import io.flow.delta.lib.BuildNames
import io.flow.delta.v0.models.{Organization, Project, Build, EventType => DeltaEventType, Visibility}
import io.flow.play.util.Config
import io.flow.travis.ci.v0.Client
import io.flow.travis.ci.v0.models._
import scala.concurrent.ExecutionContext.Implicits.global

case class TravisCiBuild(
    version: String,
    org: Organization,
    project: Project,
    build: Build,
    buildConfig: BuildConfig,
    config: Config
) extends BuildEventLog {

  private[this] val client = createClient()

  def withProject[T](f: Project => T): Option[T] = {
    Option(f(project))
  }

  def withBuild[T](f: Build => T): Option[T] = {
    Option(f(build))
  }

  def buildDockerImage() {
    val dockerImageName = BuildNames.dockerImageName(org.docker, build)

    this.synchronized {
      client.requests.get(
          repositorySlug = travisRepositorySlug(),
          limit = Option(20)
      ).map { requestGetResponse =>

        val requests = requestGetResponse.requests
          .filter(_.eventType == EventType.Api)
          .filter(_.branchName.name.getOrElse("") == version)
          .filter(_.commit.message.getOrElse("").contains(dockerImageName))

        requests match {
          case Nil => {
            // No matching builds from Travis. Check the Event log to see
            // if we tried to submit a build, otherwise submit a new build.
            EventsDao.findAll(
              projectId = Some(project.id),
              `type` = Some(DeltaEventType.Change),
              summaryKeywords = Some(travisChangedMessage(dockerImageName, version)),
              limit = 1
            ).headOption match {
              case None => {
                postBuildRequest()
              }
              case Some(_) => {
                log.checkpoint(s"Waiting for triggered build [${dockerImageName}:${version}]")
              }
            }
          }
          case requests => {
            requests.foreach { request =>
              request.builds.foreach { build =>
                log.checkpoint(s"Travis CI build [${dockerImageName}:${version}], number: ${build.number}, state: ${build.state}")
              }
            }
          }
        }

      }.recover {
        case io.flow.docker.registry.v0.errors.UnitResponse(code) => {
          log.error(s"Travis CI returned HTTP $code when fetching requests [${dockerImageName}:${version}]")
        }
        case err => {
          err.printStackTrace(System.err)
          log.error(s"Error fetching Travis CI requests [${dockerImageName}:${version}]: $err")
        }
      }
      Thread.sleep(2000)
    }
  }
  
  private def postBuildRequest() {
    val dockerImageName = BuildNames.dockerImageName(org.docker, build)

    client.requests.post(
      repositorySlug = travisRepositorySlug(),
      requestPostForm = createRequestPostForm()
    ).map { request =>
      log.changed(travisChangedMessage(dockerImageName, version))
    }.recover {
      case io.flow.docker.registry.v0.errors.UnitResponse(code) => {
        code match {
          case _ => {
            log.error(s"Travis CI returned HTTP $code when triggering build [${dockerImageName}:${version}]")
          }
        }
      }
      case err => {
        err.printStackTrace(System.err)
        log.error(s"Error triggering Travis CI build [${dockerImageName}:${version}]: $err")
      }
    }
  }

  private def createRequestPostForm(): RequestPostForm = {
    val dockerImageName = BuildNames.dockerImageName(org.docker, build)

    RequestPostForm(
      request = RequestPostFormData(
        branch = version,
        message = Option(travisCommitMessage(dockerImageName, version)),
        config = RequestConfigData(
          mergeMode = Option(MergeMode.Replace),
          dist = Option("trusty"),
          sudo = Option("required"),
          services = Option(Seq("docker")),
          addons = Option(RequestConfigAddonsData(
             apt = Option(RequestConfigAddonsAptData(
               packages = Option(Seq("docker-ce=17.05.0~ce-0~ubuntu-trusty"))
             ))
          )),
          script = Option(Seq(
            "docker --version",
            "echo TRAVIS_BRANCH=$TRAVIS_BRANCH",
            s"docker build -f ${buildConfig.dockerfile} -t ${dockerImageName}:$${TRAVIS_BRANCH} .",
            "docker login -u=$DOCKER_USERNAME -p=$DOCKER_PASSWORD",
            s"docker push ${dockerImageName}:$${TRAVIS_BRANCH}"
          ))
        )
      )
    )
  }

  private def createRequestHeaders(): Seq[(String, String)] = {
    val token = if (project.visibility == Visibility.Public) {
      config.requiredString("travis.delta.auth.token.public")
    } else {
      config.requiredString("travis.delta.auth.token.private")
    }

    Seq(
      ("Travis-API-Version", "3"),
      ("Authorization", s"token ${token}")
    )
  }

  private def createClient(): Client = {
    // Travis separates public and private projects into separate domains
    val baseUrl = if (project.visibility == Visibility.Public) {
      "https://api.travis-ci.org"
    } else {
      "https://api.travis-ci.com"
    }

    new Client(baseUrl, None, createRequestHeaders())
  }

  private def travisRepositorySlug(): String = {
    org.docker.organization + "/" + project.id
  }
  
  private def travisChangedMessage(dockerImageName: String, version: String): String = {
    s"Triggered docker build for ${dockerImageName}:${version}"
  }
  
  private def travisCommitMessage(dockerImageName: String, version: String): String = {
    s"Delta: building image ${dockerImageName}:${version}"
  }

}
