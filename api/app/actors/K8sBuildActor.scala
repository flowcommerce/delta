package io.flow.delta.actors

import akka.actor.{Actor, ActorSystem}
import db._
import io.flow.akka.SafeReceive
import io.flow.delta.lib.StateFormatter
import io.flow.delta.v0.models.Build
import io.flow.log.RollbarLogger

import scala.concurrent.Future
import scala.concurrent.duration._

object K8sBuildActor {

  trait Message

  object Messages {
    case object Setup extends Message
    case object CheckLastState extends Message
  }

  trait Factory {
    def apply(buildId: String): Actor
  }

}

class K8sBuildActor @javax.inject.Inject() (
  override val buildsDao: BuildsDao,
  override val configsDao: ConfigsDao,
  override val projectsDao: ProjectsDao,
  override val organizationsDao: OrganizationsDao,
  //buildLastStatesDao: InternalBuildLastStatesDao,
  //usersDao: UsersDao,
  system: ActorSystem,
  override val logger: RollbarLogger,
  @com.google.inject.assistedinject.Assisted buildId: String
) extends Actor with DataBuild {

  private[this] implicit val ec = system.dispatchers.lookup("build-actor-context")
  private[this] implicit val configuredRollbar = logger.fingerprint("K8sBuildActor")

  def receive = SafeReceive.withLogUnhandled {

    case K8sBuildActor.Messages.Setup =>
      handleReceiveSetupEvent()

    case K8sBuildActor.Messages.CheckLastState =>
      withEnabledBuild { build =>
        await {
          captureLastState(build)
        }
      }
  }

  private[this] def handleReceiveSetupEvent(): Unit = {
    setBuildId(buildId)

    system.scheduler.schedule(
      Duration(1L, "second"),
      Duration(EcsBuildActor.CheckLastStateIntervalSeconds, "seconds")
    ) {
      self ! K8sBuildActor.Messages.CheckLastState
    }
    ()
  }

  def captureLastState(build: Build): Future[String] = {
    // TODO: get last state from k8s
    /*
    getK8sLastState(build).map { versions =>
      buildLastStatesDao.upsert(
        usersDao.systemUser,
        build,
        StateForm(versions = versions)
      )
      StateFormatter.label(versions)
    }
  */
    println(s"TODO: captureLastState for build '${build.id}''")
    Future.successful(StateFormatter.label(Nil))
  }

}
