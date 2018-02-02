package io.flow.delta.actors

import javax.inject.Inject

import akka.actor.Actor
import db.{SubscriptionsDao, UserIdentifiersDao, UsersDao}
import io.flow.common.v0.models.{User, UserReference}
import io.flow.delta.v0.models.{Publication, SubscriptionForm}
import io.flow.play.actors.ErrorHandler

object UserActor {

  trait Message

  object Messages {
    case class Data(id: String) extends Message
    case object Created extends Message
  }

}

class UserActor @Inject()(
  subscriptionsDao: SubscriptionsDao,
  usersDao: UsersDao,
  userIdentifiersDao: UserIdentifiersDao
) extends Actor with ErrorHandler {

  var dataUser: Option[User] = None

  def receive = {

    case msg @ UserActor.Messages.Data(id) => withErrorHandler(msg) {
      dataUser = usersDao.findById(id)
    }

    case msg @ UserActor.Messages.Created => withErrorHandler(msg) {
      dataUser.foreach { user =>
        // This method will force create an identifier
        userIdentifiersDao.latestForUser(MainActor.SystemUser, UserReference(id = user.id))

        // Subscribe the user automatically to key personalized emails.
        Seq(Publication.Deployments).foreach { publication =>
          subscriptionsDao.upsert(
            MainActor.SystemUser,
            SubscriptionForm(
              userId = user.id,
              publication = publication
            )
          )
        }
      }
    }

    case msg: Any => logUnhandledMessage(msg)
  }

}
