package io.flow.delta.api.lib

import db.{UserIdentifiersDao, UsersDao}
import io.flow.common.v0.models.{Name, User, UserReference}

/**
  * Information we use to render email messages, including the links
  * to unsubscribe.
  */
case class Recipient(
  email: String,
  name: Name,
  userId: String,
  identifier: String
) {
  def fullName(): Option[String] = {
    Seq(name.first, name.last).flatten.map(_.trim).filter(!_.isEmpty).toList match {
      case Nil => None
      case names => Some(names.mkString(" "))
    }
  }
}

object Recipient {

  def fromUser(user: User): Option[Recipient] = {
    user.email.map { email =>
      Recipient(
        email = email,
        name = user.name,
        userId = user.id,
        identifier = UserIdentifiersDao.latestForUser(UsersDao.systemUser, UserReference(id = user.id)).value
      )
    }
  }

}


