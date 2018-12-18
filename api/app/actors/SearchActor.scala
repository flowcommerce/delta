package io.flow.delta.actors

import akka.actor.Actor
import db.{ItemsDao, ProjectsDao}
import io.flow.log.RollbarLogger
import io.flow.play.actors.ErrorHandler
import io.flow.postgresql.Authorization

object SearchActor {

  sealed trait Message

  object Messages {
    case class SyncProject(id: String) extends Message
  }

}

class SearchActor(
  override val logger: RollbarLogger,
  projectsDao: ProjectsDao,
  itemsDao: ItemsDao
) extends Actor with ErrorHandler {

  def receive = {

    case msg @ SearchActor.Messages.SyncProject(id) => withErrorHandler(msg) {
      projectsDao.findById(Authorization.All, id) match {
        case None => itemsDao.deleteByObjectId(Authorization.All, MainActor.SystemUser, id)
        case Some(project) => itemsDao.replaceProject(MainActor.SystemUser, project)
      }
    }

    case msg: Any => logUnhandledMessage(msg)
  }

}
