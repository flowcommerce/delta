package io.flow.delta.actors

import akka.actor.{Actor, ActorSystem}
import db.{ConfigsDao, ImagesDao, ImagesWriteDao, UsersDao}
import io.flow.delta.actors.functions.{SyncDockerImages, TravisCiBuild}
import io.flow.delta.lib.BuildNames
import io.flow.delta.v0.models._
import io.flow.delta.config.v0.models.{Build => BuildConfig}
import io.flow.docker.registry.v0.models.{BuildForm => DockerBuildForm, BuildTag => DockerBuildTag}
import io.flow.docker.registry.v0.Client
import io.flow.play.actors.ErrorHandler
import io.flow.play.util.Config
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._

import scala.util.{Failure, Success, Try}

object DockerHubActor {

  trait Message

  object Messages {

    /**
      * Message to start the build the docker image for the specified
      * version. Note the current implementation does not actually!
      * trigger a build - just watches docker until the build
      * completed - thus assuming an automated build in docker hub.
      */
    case class Build(version: String) extends Message

    case class Monitor(version: String, start: DateTime) extends Message

    case object Setup extends Message
  }

  trait Factory {
    def apply(buildId: String): Actor
  }

}

class DockerHubActor @javax.inject.Inject() (
  imagesWriteDao: ImagesWriteDao,
  system: ActorSystem,
  dockerHubToken: DockerHubToken,
  override val configsDao: ConfigsDao,
  @com.google.inject.assistedinject.Assisted buildId: String,
  config: Config
) extends Actor with ErrorHandler with DataBuild with BuildEventLog {

  private[this] implicit val ec = system.dispatchers.lookup("dockerhub-actor-context")

  private[this] val client = new Client()

  private[this] val IntervalSeconds = 30
  private[this] val TimeoutSeconds = 1500

  private[this] val BuildVersion12 = "1.2"

  def receive = {
    case msg @ DockerHubActor.Messages.Setup => withErrorHandler(msg) {
      setBuildId(buildId)
    }

    case msg @ DockerHubActor.Messages.Build(version) => withErrorHandler(msg) {
      withOrganization { org =>
        withProject { project =>
          withEnabledBuild { build =>
            withBuildConfig { buildConfig =>
              if (buildConfig.version.getOrElse(BuildVersion12) == BuildVersion12) {
                TravisCiBuild(version, org, project, build, buildConfig, config).buildDockerImage()
              } else {
                postDockerHubImageBuild(version, org, project, build, buildConfig)
              }
              self ! DockerHubActor.Messages.Monitor(version, new DateTime())
            }
          }
        }
      }
    }

    case msg @ DockerHubActor.Messages.Monitor(version, start) => withErrorHandler(msg) {
      withEnabledBuild { build =>
        withOrganization { org =>
          val imageFullName = BuildNames.dockerImageName(org.docker, build, version)

          Await.result(
            SyncDockerImages(build).run,
            Duration.Inf
          )

          ImagesDao.findByBuildIdAndVersion(build.id, version) match {
            case Some(image) => {
              log.completed(s"Docker hub image $imageFullName is ready - id[${image.id}]")
              // Don't fire an event; the ImagesDao will already have
              // raised ImageCreated
            }

            case None => {
              if (start.plusSeconds(TimeoutSeconds).isBefore(new DateTime)) {
                val ex = new java.util.concurrent.TimeoutException()
                log.error(s"Timeout after $TimeoutSeconds seconds. Docker image $imageFullName was not built")

              } else {
                log.checkpoint(s"Docker hub image $imageFullName is not ready. Will check again in $IntervalSeconds seconds")
                system.scheduler.scheduleOnce(Duration(IntervalSeconds, "seconds")) {
                  self ! DockerHubActor.Messages.Monitor(version, start)
                }
              }
            }
          }
        }
      }
    }

    case msg: Any => logUnhandledMessage(msg)
  }

  def postDockerHubImageBuild(version: String, org: Organization, project: Project, build: Build, buildConfig: BuildConfig) {
    client.DockerRepositories.postAutobuild(
      org.docker.organization,
      BuildNames.projectName(build),
      createBuildForm(org.docker, project.scms, project.uri, build, buildConfig),
      requestHeaders = dockerHubToken.requestHeaders(org.id)
    ).map { dockerHubBuild =>
      // TODO: Log the docker hub URL and not the VCS url
      log.completed(s"Docker Hub repository and automated build [${dockerHubBuild.repoWebUrl}] created.")
    }.recover {
      case io.flow.docker.registry.v0.errors.UnitResponse(code) => {
        code match {
          case 400 => // automated build already exists
          case _ => {
            log.completed(s"Docker Hub returned HTTP $code when trying to create automated build")
          }
        }
      }
      case err => {
        err.printStackTrace(System.err)
        log.completed(s"Error creating Docker Hub repository and automated build: $err", Some(err))
      }
    }
  }

  def createBuildForm(docker: Docker, scms: Scms, scmsUri: String, build: Build, config: BuildConfig): DockerBuildForm = {
    val fullName = BuildNames.dockerImageName(docker, build)
    val buildTags = createBuildTags(config.dockerfile)

    val vcsRepoName = io.flow.delta.api.lib.GithubUtil.parseUri(scmsUri) match {
      case Left(errors) => {
        Logger.warn(s"Error parsing VCS URI[$scmsUri]. defaulting vcsRepoName to[$fullName]: ${errors.mkString(", ")}")
        fullName
      }
      case Right(repo) => {
        repo.toString
      }
    }

    DockerBuildForm(
      active = true,
      buildTags = buildTags,
      description = s"Automated build for $fullName",
      dockerhubRepoName = fullName,
      isPrivate = true,
      name = BuildNames.projectName(build),
      namespace = docker.organization,
      provider = scms match {
        case Scms.Github => "github"
        case Scms.UNDEFINED(other) => other
      },
      vcsRepoName = vcsRepoName
    )
  }

  def createBuildTags(dockerfilePath: String): Seq[DockerBuildTag] = {
    Seq(
      DockerBuildTag(
        dockerfileLocation = dockerfilePath.replace("./Dockerfile", "").replace("/Dockerfile", "").replace("Dockerfile", ""),
        name = "{sourceref}",
        sourceName = "/^[0-9]+\\.[0-9]+\\.[0-9]+$/",
        sourceType = "Tag"
      )
    )
  }
}
