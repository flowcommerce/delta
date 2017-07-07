package io.flow.delta.actors.functions

import io.flow.delta.v0.models._
import io.flow.delta.config.v0.models.{Build => BuildConfig}
import io.flow.delta.lib.BuildNames
import io.flow.play.util.Config
import io.flow.travis.ci.v0.Client
import io.flow.travis.ci.v0.models._
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global

case class TravisCiBuild() {
  
  private[this] val client = new Client()
  
  def buildDockerImage(version: String, org: Organization, project: Project, build: Build, buildConfig: BuildConfig, config: Config) {
    val repositorySlug = travisRepositorySlug(org, project)
    val dockerImageName = BuildNames.dockerImageName(org.docker, build)

    client.requests.post(
        repositorySlug = repositorySlug,
        requestPostForm = createRequestPostForm(version, org, project, build, buildConfig, config),
        requestHeaders = createRequestHeaders(version, org, project, build, buildConfig, config)
    ).map { request =>
      Logger.info(s">>>>>>>> Travis CI build triggered [${dockerImageName}:${version}] <<<<<<<<")
    }.recover {
      case io.flow.docker.registry.v0.errors.UnitResponse(code) => {
        code match {
          case _ => {
            Logger.info(s">>>>>>>> Travis CI returned HTTP $code when triggering build <<<<<<<<")
          }
        }
      }
      case err => {
        err.printStackTrace(System.err)
        Logger.info(s">>>>>>>> Error triggering Travis CI build: $err")
      }
    }
  }

  private def createRequestPostForm(version: String, org: Organization, project: Project, build: Build, buildConfig: BuildConfig, config: Config): RequestPostForm = {
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
  
  private def createRequestHeaders(version: String, org: Organization, project: Project, build: Build, buildConfig: BuildConfig, config: Config): Seq[(String, String)] = {
    val token = config.requiredString("travis.delta.auth.token")
    Seq(
      ("Travis-API-Version", "3"),
      ("Authorization", s"token ${token}")
    )
  }

  private def travisRepositorySlug(org: Organization, project: Project): String = {
    org.docker.organization + "/" + project.id
  }
}