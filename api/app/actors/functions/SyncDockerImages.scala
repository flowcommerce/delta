package io.flow.delta.actors.functions

import javax.inject.Inject

import db.{ImagesDao, ImagesWriteDao, OrganizationsDao}
import io.flow.delta.actors.{BuildSupervisorFunction, DockerHubToken, SupervisorResult}
import io.flow.delta.config.v0.models.{BuildConfig, BuildStage}
import io.flow.delta.lib.{BuildNames, Semver}
import io.flow.delta.v0.models.{Build, Docker, ImageForm}
import io.flow.docker.registry.v0.Client
import io.flow.util.Constants
import io.flow.postgresql.Authorization
import play.api.Application
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

object SyncDockerImages extends BuildSupervisorFunction {

  override val stage = BuildStage.SyncDockerImage

  override def run(
    build: Build,
    cfg: BuildConfig,
  ) (
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult] = {
    val syncDockerImages = app.injector.instanceOf[SyncDockerImages]
    syncDockerImages.run(build, cfg)
  }

}

/**
  * Downloads all tags from docker hub and stores in local DB
  */
class SyncDockerImages @Inject()(
  dockerHubToken: DockerHubToken,
  imagesDao: ImagesDao,
  imagesWriteDao: ImagesWriteDao,
  organizationsDao: OrganizationsDao,
  wSClient: WSClient
) {
  private[this] val client = new Client(ws = wSClient)

  def run(build: Build, cfg: BuildConfig)(
    implicit ec: ExecutionContext
  ): Future[SupervisorResult] = {
    organizationsDao.findById(Authorization.All, build.project.organization.id) match {
      case None =>{
        // build was deleted
        Future(SupervisorResult.Ready(s"Build org[${build.project.organization.id}] not found - nothing to do"))
      }

      case Some(org) => {
        syncImages(org.docker, build, cfg)
      }
    }
  }

  def syncImages(
    docker: Docker,
    build: Build,
    cfg: BuildConfig,
  ) (
    implicit ec: ExecutionContext
  ): Future[SupervisorResult] = {
    client.V2Tags.get(
      docker.organization,
      BuildNames.projectName(build),
      requestHeaders = dockerHubToken.requestHeaders(build.project.organization.id)
    ).map { tags =>
      val newTags: Seq[String] = tags.results.filter(t => Semver.isSemver(t.name)).flatMap { tag =>
        if (upsertImage(docker, build, cfg, tag.name)) {
          Some(tag.name)
        } else {
          None
        }
      }
      newTags.toList match {
        case Nil => SupervisorResult.Ready("No new docker images found")
        case tag :: Nil => SupervisorResult.Change(s"Docker image[$tag] synced")
        case multiple => SupervisorResult.Change(s"Docker images[${multiple.mkString(", ")}] synced")
      }

    }.recover {
      case io.flow.docker.registry.v0.errors.UnitResponse(401) => {
        SupervisorResult.Error(s"${BuildNames.projectName(build)} HTTP 401 while fetching docker tags for build id[${build.id}]")
      }

      case io.flow.docker.registry.v0.errors.UnitResponse(404) => {
        SupervisorResult.Ready("No docker images found")
      }

      case ex: Throwable => {
        ex.printStackTrace(System.err)
        SupervisorResult.Error(s"${BuildNames.projectName(build)} Error fetching docker tags for build id[${build.id}]", Some(ex))
      }
    }
  }

  private[this] def upsertImage(docker: Docker, build: Build, cfg: BuildConfig, version: String): Boolean = {
    imagesDao.findByBuildIdAndVersion(build.id, version) match {
      case Some(_) => {
        // Already know about this tag
        false
      }

      case None => {
        imagesWriteDao.create(
          Constants.SystemUser,
          ImageForm(
            buildId = build.id,
            name = BuildNames.dockerImageName(docker, build, cfg),
            version = version
          )
        ) match {
          case Left(msgs) => sys.error(msgs.mkString(", "))
          case Right(_) => true
        }
      }
    }
  }

}
