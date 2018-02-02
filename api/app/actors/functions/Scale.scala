package io.flow.delta.actors.functions

import db.{BuildLastStatesDao, BuildDesiredStatesDao}
import io.flow.delta.actors.{MainActor, MainActorProvider, BuildActor, BuildSupervisorFunction, SupervisorResult}
import io.flow.delta.config.v0.models.BuildStage
import io.flow.postgresql.Authorization
import io.flow.delta.v0.models.Build
import org.joda.time.DateTime
import scala.concurrent.Future

/**
  * If we have both an desired state and a recent actual state,
  * compares the two to see if there are any instances we need to
  * scale up or down in production. Scale Up will always happen first;
  * scale down only initiatied after Scale Up is complete.
  */
object Scale extends BuildSupervisorFunction {

  private[this] val SecondsUntilStale = (BuildActor.CheckLastStateIntervalSeconds * 2.5).toInt

  override val stage = BuildStage.Scale

  override def run(
    build: Build
  ) (
    implicit ec: scala.concurrent.ExecutionContext
  ): Future[SupervisorResult] = {
    val lastState = BuildLastStatesDao.findByBuildId(Authorization.All, build.id)
    val desiredState = BuildDesiredStatesDao.findByBuildId(Authorization.All, build.id)
    Future {
      (lastState, desiredState) match {

        case (_, None) => {
          SupervisorResult.Error("Desired state is not known")
        }

        case (None, Some(_)) => {
          MainActorProvider.ref ! MainActor.Messages.CheckLastState(build.id)
          SupervisorResult.Checkpoint(s"Requested CheckLastState as last state is not known")
        }

        case (Some(last), Some(desired)) => {
          Scale.isRecent(last.timestamp) match {
            case false => {
              MainActorProvider.ref ! MainActor.Messages.CheckLastState(build.id)
              SupervisorResult.Error(s"Requested CheckLastState as last state is too old[${last.timestamp}]")
            }
            case true => {
              Deployer(build, last, desired).scale()
            }
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

