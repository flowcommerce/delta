package db

import io.flow.delta.v0.models.UserIdentifier
import io.flow.common.v0.models.User
import io.flow.postgresql.Authorization

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class UserIdentifiersDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createUserIdentifier(): (User, UserIdentifier) = {
    val user = createUser()
    val userIdentifier = UserIdentifiersDao.createForUser(systemUser, user)
    (user, userIdentifier)
  }

  "createForUser" in {
    val user = createUser()
    val identifier1 = UserIdentifiersDao.createForUser(systemUser, user)
    val identifier2 = UserIdentifiersDao.createForUser(systemUser, user)

    identifier1.value must not be(identifier2.value)
    identifier1.user.id must be(user.id)
    identifier2.user.id must be(user.id)
    identifier1.value.length must be(60)
  }

  "findById" in {
    val (user, identifier) = createUserIdentifier()

    UserIdentifiersDao.findById(Authorization.All, identifier.id).map(_.id) must be(
      Some(identifier.id)
    )

    UserIdentifiersDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "findAll" must {
    "filter by ids" in {
      val (user1, identifier1) = createUserIdentifier()
      val (user2, identifier2) = createUserIdentifier()

      UserIdentifiersDao.findAll(Authorization.All, ids = Some(Seq(identifier1.id, identifier2.id))).map(_.id).sorted must be(
        Seq(identifier1.id, identifier2.id).sorted
      )

      UserIdentifiersDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
      UserIdentifiersDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
      UserIdentifiersDao.findAll(Authorization.All, ids = Some(Seq(identifier1.id, UUID.randomUUID.toString))).map(_.id) must be(
        Seq(identifier1.id)
      )
    }

    "filter by identifier" in {
      val (user, identifier) = createUserIdentifier()

      UserIdentifiersDao.findAll(Authorization.All, value = Some(identifier.value)).map(_.id) must be(Seq(identifier.id))
      UserIdentifiersDao.findAll(Authorization.All, value = Some(createTestKey())) must be(Nil)
    }
  }
}
