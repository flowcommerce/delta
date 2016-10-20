package io.flow.delta.actors.functions

import db.{ImagesDao, ImagesWriteDao, OrganizationsDao, UsersDao}
import io.flow.delta.actors.{BuildSupervisorFunction, DockerHubToken, SupervisorResult}
import io.flow.delta.config.v0.models.BuildStage
import io.flow.delta.lib.{BuildNames, Semver}
import io.flow.docker.registry.v0.Client
import io.flow.delta.v0.models.{Build, Docker, ImageForm}
import io.flow.postgresql.Authorization
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}

/**
  * Downloads all tags from docker hub and stores in local DB
  */
object SyncDockerImages extends BuildSupervisorFunction {

  override val stage = BuildStage.SyncDockerImage

  override def run(
    build: Build
  ) (
    implicit ec: ExecutionContext
  ): Future[SupervisorResult] = {
    SyncDockerImages(build).run
  }

}

case class SyncDockerImages(build: Build) {

  private[this] def imagesWriteDao = play.api.Play.current.injector.instanceOf[ImagesWriteDao]
  private[this] def dockerHubToken = play.api.Play.current.injector.instanceOf[DockerHubToken]
  private[this] val client = new Client()

  def run(
    implicit ec: ExecutionContext
  ): Future[SupervisorResult] = {
    OrganizationsDao.findById(Authorization.All, build.project.organization.id) match {
      case None =>{
        // build was deleted
        Future(SupervisorResult.Ready(s"Build org[${build.project.organization.id}] not found - nothing to do"))
      }

      case Some(org) => {
        syncImages(org.docker, build)
      }
    }
  }

  def syncImages(
    docker: Docker,
    build: Build
  ) (
    implicit ec: ExecutionContext
  ): Future[SupervisorResult] = {
    client.V2Tags.get(
      docker.organization,
      BuildNames.projectName(build),
      requestHeaders = dockerHubToken.requestHeaders(build.project.organization.id)
    ).map { tags =>
      val newTags: Seq[String] = tags.results.filter(t => Semver.isSemver(t.name)).flatMap { tag =>
        if (upsertImage(docker, build, tag.name)) {
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
      case ex: Throwable => {
        ex.printStackTrace(System.err)
        SupervisorResult.Error(s"${BuildNames.projectName(build)} Error fetching docker tags for build id[${build.id}]", Some(ex))
      }
    }
  }

  private[this] def upsertImage(docker: Docker, build: Build, version: String): Boolean = {
    ImagesDao.findByBuildIdAndVersion(build.id, version) match {
      case Some(_) => {
        // Already know about this tag
        false
      }

      case None => {
        imagesWriteDao.create(
          UsersDao.systemUser,
          ImageForm(
            buildId = build.id,
            name = BuildNames.dockerImageName(docker, build),
            version = version
          )
        ) match {
          case Left(msgs) => sys.error(msgs.mkString(", "))
          case Right(img) => true
        }
      }
    }
  }
  
}
