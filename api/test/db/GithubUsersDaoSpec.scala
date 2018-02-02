package db

import io.flow.delta.v0.models.UserForm
import io.flow.common.v0.models.Name
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class GithubUsersDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "upsertById" in {
    val form = createGithubUserForm()
    val user1 = GithubUsersDao.create(None, form)

    val user2 = GithubUsersDao.upsertById(None, form)
    user1.id must be(user2.id)

    val user3 = GithubUsersDao.upsertById(Some(systemUser), createGithubUserForm())

    user2.id must not be(user3.id)
    user2.id must not be(user3.id)
  }

  "findById" in {
    val user = createGithubUser()
    GithubUsersDao.findById(user.id).map(_.id) must be(
      Some(user.id)
    )

    UsersDao.findById(UUID.randomUUID.toString) must be(None)
  }

  "findAll by ids" in {
    val user1 = createGithubUser()
    val user2 = createGithubUser()

    GithubUsersDao.findAll(id = Some(Seq(user1.id, user2.id))).map(_.id) must be(
      Seq(user1.id, user2.id)
    )

    GithubUsersDao.findAll(id = Some(Nil)) must be(Nil)
    GithubUsersDao.findAll(id = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    GithubUsersDao.findAll(id = Some(Seq(user1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(user1.id))
  }

}
