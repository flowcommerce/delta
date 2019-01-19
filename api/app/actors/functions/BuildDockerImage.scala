package io.flow.delta.actors.functions

import javax.inject.Inject

import db.{BuildDesiredStatesDao, EventsDao, ImagesDao}
import io.flow.delta.actors.{BuildSupervisorFunction, MainActor, SupervisorResult}
import io.flow.delta.config.v0.models.BuildStage
import io.flow.delta.lib.Text
import io.flow.delta.v0.models.{Build, EventType}
import io.flow.postgresql.Authorization
import play.api.Application

import scala.concurrent.Future

object BuildDockerImage extends BuildSupervisorFunction {

  override val stage = BuildStage.BuildDockerImage

  override def run(
    build: Build
  ) (
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult] = Future {
    val buildDockerImage = app.injector.instanceOf[BuildDockerImage]
    buildDockerImage.run(build)
  }

}

/**
  * Looks up the desired state for a build. If found, checks to see
  * if we already have a docker image locally for each version in the
  * desired stated, triggering docker builds for each image that is
  * not found locally.
  */
class BuildDockerImage @Inject()(
  buildDesiredStatesDao: BuildDesiredStatesDao,
  eventsDao: EventsDao,
  imagesDao: ImagesDao,
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef
) {
  def run(build: Build): SupervisorResult = {
    buildDesiredStatesDao.findByBuildId(Authorization.All, build.id) match {
      case None => {
        SupervisorResult.Error("Build does not have a desired state")
      }

      case Some(state) => {
        val versions = state.versions.flatMap { version =>
          imagesDao.findByBuildIdAndVersion(build.id, version.name) match {
            case Some(_) => {
              None
            }
            case None => {
              mainActor ! MainActor.Messages.BuildDockerImage(build.id, version.name)
              Some(version.name)
            }
          }
        }

        versions.toList match {
          case Nil => {
            SupervisorResult.Ready(s"All images exist for versions in desired state[%s]".format(state.versions.map(_.name).mkString(", ")))
          }
          case _ => {
            val label = Text.pluralize(versions.size.toLong, "docker image", "docker images") + ": " + versions.mkString(", ")
            val msg = s"Started build of $label"

            eventsDao.findAll(
              projectId = Some(build.project.id),
              `type` = Some(EventType.Change),
              summaryKeywords = Some(msg),
              limit = 1
            ).headOption match {
              case None => SupervisorResult.Change(msg)
              case Some(_) => SupervisorResult.Checkpoint(s"Waiting for build of $label")
            }
          }
        }
      }
    }
  }

}
