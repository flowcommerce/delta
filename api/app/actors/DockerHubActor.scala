package io.flow.delta.actors

import akka.actor.{Actor, ActorSystem}
import db._
import io.flow.akka.SafeReceive
import io.flow.delta.actors.functions.{SyncDockerImages, TravisCiBuild, TravisCiDockerImageBuilder}
import io.flow.delta.api.lib.EventLogProcessor
import io.flow.delta.config.v0.models.{Build => BuildConfig}
import io.flow.delta.lib.BuildNames
import io.flow.delta.v0.models._
import io.flow.docker.registry.v0.Client
import io.flow.docker.registry.v0.models.{BuildForm => DockerBuildForm, BuildTag => DockerBuildTag}
import io.flow.log.RollbarLogger
import org.joda.time.DateTime
import play.api.libs.ws.WSClient

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

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
  @com.google.inject.assistedinject.Assisted buildId: String,
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  dockerHubToken: DockerHubToken,
  imagesDao: ImagesDao,
  eventLogProcessor: EventLogProcessor,
  syncDockerImages: SyncDockerImages,
  system: ActorSystem,
  travisCiDockerImageBuilder: TravisCiDockerImageBuilder,
  wSClient: WSClient,
  override val logger: RollbarLogger
) extends Actor with DataBuild {

  private[this] implicit val ec = system.dispatchers.lookup("dockerhub-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("DockerHubActor")

  private[this] val client = new Client(ws = wSClient)

  private[this] val intervalSeconds = 30L
  private[this] val timeoutSeconds = 1500

  def receive = SafeReceive.withLogUnhandled {
    case DockerHubActor.Messages.Setup =>
      setBuildId(buildId)

    case DockerHubActor.Messages.Build(version) =>
      handleBuildEvent(version)

    case DockerHubActor.Messages.Monitor(version, start) =>
      handleMonitorEvent(version, start)
  }

  private def handleBuildEvent(version: String) = {
    withOrganization { org =>
      withProject { project =>
        withEnabledBuild { build =>
          withBuildConfig { buildConfig =>
            travisCiDockerImageBuilder.buildDockerImage(TravisCiBuild(version, org, project, build, buildConfig, wSClient))
            self ! DockerHubActor.Messages.Monitor(version, new DateTime())
          }
        }
      }
    }
  }

  private def handleMonitorEvent(version: String, start: DateTime) = {
    withEnabledBuild { build =>
      withOrganization { org =>
        val imageFullName = BuildNames.dockerImageName(org.docker, build, requiredBuildConfig, version)

        Await.result(
          syncDockerImages.run(build),
          Duration.Inf
        )

        val projectId = build.project.id

        imagesDao.findByBuildIdAndVersion(build.id, version) match {
          case Some(image) => {
            eventLogProcessor.completed(s"Docker hub image $imageFullName is ready - id[${image.id}]", log = log(projectId))
            // Don't fire an event; the ImagesDao will already have
            // raised ImageCreated
          }

          case None => {
            if (start.plusSeconds(timeoutSeconds).isBefore(new DateTime)) {
              eventLogProcessor.error(s"Timeout after $timeoutSeconds seconds. Docker image $imageFullName was not built", log = log(projectId))

            } else {
              eventLogProcessor.checkpoint(s"Docker hub image $imageFullName is not ready. Will check again in $intervalSeconds seconds", log = log(projectId))
              system.scheduler.scheduleOnce(Duration(intervalSeconds, "seconds")) {
                self ! DockerHubActor.Messages.Monitor(version, start)
              }
            }
          }
        }
      }
    }
  }

  def postDockerHubImageBuild(org: Organization, project: Project, build: Build, buildConfig: BuildConfig): Future[Unit] = {
    client.DockerRepositories.postAutobuild(
      org.docker.organization,
      BuildNames.projectName(build),
      createBuildForm(org.docker, project.scms, project.uri, build, buildConfig),
      requestHeaders = dockerHubToken.requestHeaders(org.id)
    ).map { dockerHubBuild =>
      // TODO: Log the docker hub URL and not the VCS url
      eventLogProcessor.completed(s"Docker Hub repository and automated build [${dockerHubBuild.repoWebUrl}] created.", log = log(project.id))
    }.recover {
      case io.flow.docker.registry.v0.errors.UnitResponse(code) => {
        code match {
          case 400 => // automated build already exists
          case _ => {
            eventLogProcessor.completed(s"Docker Hub returned HTTP $code when trying to create automated build", log = log(project.id))
          }
        }
      }
      case err => {
        err.printStackTrace(System.err)
        eventLogProcessor.completed(s"Error creating Docker Hub repository and automated build: $err", Some(err), log = log(project.id))
      }
    }
  }

  def createBuildForm(docker: Docker, scms: Scms, scmsUri: String, build: Build, config: BuildConfig): DockerBuildForm = {
    val fullName = BuildNames.dockerImageName(docker, build, requiredBuildConfig)
    val buildTags = createBuildTags(config.dockerfile)

    val vcsRepoName = io.flow.delta.api.lib.GithubUtil.parseUri(scmsUri) match {
      case Left(errors) => {
        logger.withKeyValue("scms_uri", scmsUri).withKeyValue("full_name", fullName).withKeyValue("errors", errors).warn(s"Error parsing VCS URI. defaulting vcsRepoName to full name")
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
