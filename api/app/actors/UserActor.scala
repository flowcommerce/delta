package io.flow.delta.actors

import javax.inject.Inject
import akka.actor.Actor
import db.{SubscriptionsDao, UserIdentifiersDao, UsersDao}
import io.flow.akka.SafeReceive
import io.flow.common.v0.models.{User, UserReference}
import io.flow.delta.v0.models.{Publication, SubscriptionForm}
import io.flow.log.RollbarLogger

object UserActor {

  trait Message

  object Messages {
    case class Data(id: String) extends Message
    case object Created extends Message
  }

  trait Factory {
    def apply(id: String): Actor
  }

}

class UserActor @Inject()(
  logger: RollbarLogger,
  subscriptionsDao: SubscriptionsDao,
  usersDao: UsersDao,
  userIdentifiersDao: UserIdentifiersDao,
) extends Actor {
  private[this] implicit val configuredRollbar = logger.fingerprint("UserActor")

  var dataUser: Option[User] = None

  def receive = SafeReceive.withLogUnhandled {

    case UserActor.Messages.Data(id) =>
      dataUser = usersDao.findById(id)

    case UserActor.Messages.Created =>
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

}
