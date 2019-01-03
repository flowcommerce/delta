package io.flow.delta.actors.functions

import javax.inject.Inject
import db.{BuildDesiredStatesDao, BuildLastStatesDao}
import io.flow.delta.actors.{BuildActor, BuildSupervisorFunction, MainActor, SupervisorResult}
import io.flow.delta.config.v0.models.BuildStage
import io.flow.delta.v0.models.Build
import io.flow.postgresql.Authorization
import org.joda.time.DateTime
import play.api.Application

import scala.concurrent.Future

object Scale extends BuildSupervisorFunction {

  override val stage = BuildStage.Scale

  override def run(
    build: Build
  ) (
    implicit ec: scala.concurrent.ExecutionContext, app: Application
  ): Future[SupervisorResult] = Future {
    val scale = app.injector.instanceOf[Scale]
    scale.run(build)
  }

}

/**
  * If we have both an desired state and a recent actual state,
  * compares the two to see if there are any instances we need to
  * scale up or down in production. Scale Up will always happen first;
  * scale down only initiated after Scale Up is complete.
  */
class Scale @Inject()(
  buildDesiredStatesDao: BuildDesiredStatesDao,
  buildLastStatesDao: BuildLastStatesDao,
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef
) {

  private[this] val SecondsUntilStale = (BuildActor.CheckLastStateIntervalSeconds * 2.5).toInt

  def run(
    build: Build
  ): SupervisorResult = {
    val lastState = buildLastStatesDao.findByBuildId(Authorization.All, build.id)
    val desiredState = buildDesiredStatesDao.findByBuildId(Authorization.All, build.id)
    (lastState, desiredState) match {

      case (_, None) => {
        SupervisorResult.Error("Desired state is not known")
      }

      case (None, Some(_)) => {
        mainActor ! MainActor.Messages.CheckLastState(build.id)
        SupervisorResult.Checkpoint(s"Requested CheckLastState as last state is not known")
      }

      case (Some(last), Some(desired)) => {
        isRecent(last.timestamp) match {
          case false => {
            mainActor ! MainActor.Messages.CheckLastState(build.id)
            SupervisorResult.Error(s"Requested CheckLastState as last state is too old[${last.timestamp}]")
          }
          case true => {
            Deployer(build, last, desired, mainActor).scale()
          }
        }
      }
    }
  }

  def isRecent(ts: DateTime): Boolean = {
    val now = new DateTime()
    ts.isAfter(now.minusMinutes(SecondsUntilStale))
  }
  
}

