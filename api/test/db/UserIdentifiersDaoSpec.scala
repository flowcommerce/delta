package db

import java.util.UUID

import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.UserIdentifier
import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec

class UserIdentifiersDaoSpec extends FlowPlaySpec with Helpers {

  def createUserIdentifier(): (UserReference, UserIdentifier) = {
    val user = createUserReference()
    val userIdentifier = userIdentifiersDao.createForUser(systemUser, user)
    (user, userIdentifier)
  }

  "createForUser" in {
    val user = createUserReference()
    val identifier1 = userIdentifiersDao.createForUser(systemUser, user)
    val identifier2 = userIdentifiersDao.createForUser(systemUser, user)

    identifier1.value must not be(identifier2.value)
    identifier1.user.id must be(user.id)
    identifier2.user.id must be(user.id)
    identifier1.value.length must be(60)
  }

  "findById" in {
    val (user, identifier) = createUserIdentifier()

    userIdentifiersDao.findById(Authorization.All, identifier.id).map(_.id) must be(
      Some(identifier.id)
    )

    userIdentifiersDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findAll" must {
    "filter by ids" in {
      val (user1, identifier1) = createUserIdentifier()
      val (user2, identifier2) = createUserIdentifier()

      userIdentifiersDao.findAll(Authorization.All, ids = Some(Seq(identifier1.id, identifier2.id))).map(_.id).sorted must be(
        Seq(identifier1.id, identifier2.id).sorted
      )

      userIdentifiersDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
      userIdentifiersDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
      userIdentifiersDao.findAll(Authorization.All, ids = Some(Seq(identifier1.id, UUID.randomUUID.toString))).map(_.id) must be(
        Seq(identifier1.id)
      )
    }

    "filter by identifier" in {
      val (user, identifier) = createUserIdentifier()

      userIdentifiersDao.findAll(Authorization.All, value = Some(identifier.value)).map(_.id) must be(Seq(identifier.id))
      userIdentifiersDao.findAll(Authorization.All, value = Some(createTestKey())) must be(Nil)
    }
  }
}
