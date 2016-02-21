package io.flow.delta.actors

import db.{EventsDao, ProjectsDao}
import io.flow.play.actors.ErrorHandler
import io.flow.postgresql.{Authorization, OrderBy, Pager}
import play.api.Logger
import akka.actor.Actor

object PeriodicActor {

  sealed trait Message

  object Messages {
    case object CheckProjects extends Message
    case object Startup extends Message
  }

}

class PeriodicActor extends Actor with ErrorHandler {

  private[this] val MinutesUntilInactive = 15
  
  def receive = {

    case msg @ PeriodicActor.Messages.CheckProjects => withVerboseErrorHandler(msg) {
      Pager.create { offset =>
        ProjectsDao.findAll(Authorization.All, offset = offset)
      }.foreach { project =>
        isActive(project.id) match {
          case true => {
            Logger.info(s"PeriodicActor: Project[${project.id}] is currently active - skipping sync")
          }
          case false => {
            sender ! MainActor.Messages.ProjectSync(project.id)
          }
        }
      }
    }

    case msg @ PeriodicActor.Messages.Startup => withVerboseErrorHandler(msg) {
      Pager.create { offset =>
        ProjectsDao.findAll(Authorization.All, offset = offset)
      }.foreach { project =>
        sender ! MainActor.Messages.ProjectSync(project.id)
      }
    }

    case msg: Any => logUnhandledMessage(msg)
  }


  /**
   * A project is considered active if:
   * 
   *   - it has had at least one SupervisorActor.StartedMessage log
   *     entry written in the past MinutesUntilInactive minutes
   * 
   * Otherwise, the project is not active
   */
  private[this] def isActive(projectId: String): Boolean = {
    EventsDao.findAll(
      projectId = Some(projectId),
      numberMinutesSinceCreation = Some(MinutesUntilInactive),
      summary = Some(SupervisorActor.StartedMessage),
      limit = 1
    ).headOption match {
      case None => false
      case Some(_) => true
    }
  }
  
}
