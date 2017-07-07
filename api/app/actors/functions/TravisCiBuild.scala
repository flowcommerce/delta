package io.flow.delta.actors.functions

import io.flow.delta.actors.BuildEventLog
import io.flow.delta.config.v0.models.{Build => BuildConfig}
import io.flow.delta.lib.BuildNames
import io.flow.delta.v0.models._
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
  
  private[this] val client = new Client()

  def withProject[T](f: Project => T): Option[T] = {
    Option(f(project))
  }

  def withBuild[T](f: Build => T): Option[T] = {
    Option(f(build))
  }

  def buildDockerImage() {
      val dockerImageName = BuildNames.dockerImageName(org.docker, build)

    client.requests.post(
        repositorySlug = travisRepositorySlug(),
        requestPostForm = createRequestPostForm(),
        requestHeaders = createRequestHeaders()
    ).map { request =>
      log.changed(s"Triggered build for ${dockerImageName}:${version}")
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
        message = Option(s"Delta: building image ${dockerImageName}:${version}"),
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
    val token = config.requiredString("travis.delta.auth.token")
    Seq(
      ("Travis-API-Version", "3"),
      ("Authorization", s"token ${token}")
    )
  }

  private def travisRepositorySlug(): String = {
    org.docker.organization + "/" + project.id
  }
}