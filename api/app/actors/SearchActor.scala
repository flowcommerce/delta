package io.flow.delta.actors

import akka.actor.Actor
import db.{ItemsDao, ProjectsDao}
import io.flow.akka.SafeReceive
import io.flow.log.RollbarLogger
import io.flow.postgresql.Authorization

object SearchActor {

  sealed trait Message

  object Messages {
    case class SyncProject(id: String) extends Message
  }

}

class SearchActor(
  logger: RollbarLogger,
  projectsDao: ProjectsDao,
  itemsDao: ItemsDao
) extends Actor {

  private[this] implicit val configuredRollbar = logger.fingerprint("SearchActor")

  def receive = SafeReceive.withLogUnhandled {
    case SearchActor.Messages.SyncProject(id) =>
      projectsDao.findById(Authorization.All, id) match {
        case None => itemsDao.deleteByObjectId(Authorization.All, MainActor.SystemUser, id)
        case Some(project) => itemsDao.replaceProject(MainActor.SystemUser, project)
      }
      ()
  }

}
