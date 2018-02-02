package db

import java.util.UUID

import io.flow.common.v0.models.Name
import io.flow.test.utils.FlowPlaySpec

class UsersDaoSpec extends FlowPlaySpec with Helpers {

  "Special users" must {
    "anonymous user exists" in {
      usersDao.findById(usersDao.anonymousUser.id).get.email must be(
        Some(usersDao.AnonymousEmailAddress)
      )
    }

    "system user exists" in {
      usersDao.findById(usersDao.systemUser.id).get.email must be(
        Some(usersDao.SystemEmailAddress)
      )
    }

    "system and anonymous users are different" in {
      usersDao.AnonymousEmailAddress must not be(
        usersDao.SystemEmailAddress
      )

      usersDao.anonymousUser.id must not be(
        usersDao.systemUser.id
      )
    }

  }

  "findByEmail" in {
    usersDao.findByEmail(usersDao.SystemEmailAddress).flatMap(_.email) must be(
      Some(usersDao.SystemEmailAddress)
    )

    usersDao.findByEmail(UUID.randomUUID.toString) must be(None)
  }

  "findByToken" in {
    val user = createUserReference()
    val token = createToken(createTokenForm(user = user))
    val clear = tokensDao.addCleartextIfAvailable(systemUser, token).cleartext.getOrElse {
      sys.error("Could not find cleartext of token")
    }

    usersDao.findByToken(clear).map(_.id) must be(Some(user.id))
    usersDao.findByToken(UUID.randomUUID.toString) must be(None)
  }

  "findById" in {
    usersDao.findById(usersDao.systemUser.id).map(_.id) must be(
      Some(usersDao.systemUser.id)
    )

    usersDao.findById(UUID.randomUUID.toString) must be(None)
  }

  "findByGithubUserId" in {
    val user = createUserReference()
    val githubUser = createGithubUser(createGithubUserForm(user = user))

    usersDao.findByGithubUserId(githubUser.githubUserId).map(_.id) must be(
      Some(user.id)
    )

    usersDao.findByGithubUserId(0) must be(None)
  }


  "findAll" must {

    "filter by ids" in {
      val user1 = createUser()
      val user2 = createUser()

      usersDao.findAll(ids = Some(Seq(user1.id, user2.id))).map(_.id) must be(
        Seq(user1.id, user2.id)
      )

      usersDao.findAll(ids = Some(Nil)) must be(Nil)
      usersDao.findAll(ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
      usersDao.findAll(ids = Some(Seq(user1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(user1.id))
    }

    "filter by email" in {
      val user = createUser()
      val email = user.email.getOrElse {
        sys.error("user must have email address")
      }

      usersDao.findAll(id = Some(user.id), email = Some(email)).map(_.id) must be(Seq(user.id))
      usersDao.findAll(id = Some(user.id), email = Some(createTestEmail())) must be(Nil)
    }

    "filter by identifier" in {
      val user = createUserReference()
      val identifier = userIdentifiersDao.latestForUser(systemUser, user).value

      usersDao.findAll(identifier = Some(identifier)).map(_.id) must be(Seq(user.id))
      usersDao.findAll(identifier = Some(createTestKey())) must be(Nil)
    }

  }

  "create" must {
    "user with email and name" in {
      val email = createTestEmail()
      val name = Name(
        first = Some("Michael"),
        last = Some("Bryzek")
      )
      usersWriteDao.create(
        createdBy = None,
        form = createUserForm(
          email = email,
          name = Some(name)
        )
      ) match {
        case Left(errors) => fail(errors.mkString(", "))
        case Right(user) => {
          user.email must be(Some(email))
          user.name.first must be(name.first)
          user.name.last must be(name.last)
        }
      }
    }

    "processes empty name" in {
      val name = Name(
        first = Some("  "),
        last = Some("   ")
      )
      usersWriteDao.create(
        createdBy = None,
        form = createUserForm().copy(name = Some(name))
      ) match {
        case Left(errors) => fail(errors.mkString(", "))
        case Right(user) => {
          user.name must be(Name(first = None, last = None))
        }
      }
    }

  }
}
