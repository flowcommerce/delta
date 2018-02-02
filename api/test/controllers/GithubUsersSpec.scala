package controllers

import db.GithubUsersDao
import io.flow.delta.api.lib.MockGithubData
import io.flow.delta.v0.Client
import io.flow.delta.v0.models.GithubAuthenticationForm
import io.flow.play.util.Validation
import io.flow.github.v0.models.OwnerType
import io.flow.github.v0.models.{User => GithubUser}

import java.util.UUID
import play.api.libs.ws._
import play.api.test._

class GithubUsersSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createLocalGithubUser(): GithubUser = {
    val login = createTestKey()
    GithubUser(
      id = random.positiveLong(),
      login = login,
      name = None,
      email = Some(createTestEmail()),
      avatarUrl = None,
      gravatarId = None,
      url = s"https://github.com/$login",
      htmlUrl = s"https://github.com/$login",
      `type` = OwnerType.User
    )
  }

  "POST /authentications/github with valid token" in new WithServer(port=port) {
    val githubUser = createLocalGithubUser()
    val code = "test"

    MockGithubData.addUser(githubUser, code)

    val user = await(anonClient.githubUsers.postGithub(GithubAuthenticationForm(code = code)))
    user.email must beEqualTo(githubUser.email)

    GithubUsersDao.findAll(userId = Some(user.id), limit = 1).headOption.map(_.user.id) must beEqualTo(Some(user.id))

    // Test idempotence
    val user2 = await(anonClient.githubUsers.postGithub(GithubAuthenticationForm(code = code)))
    user2.email must beEqualTo(githubUser.email)
  }

  "POST /authentications/github accepts account w/out email" in new WithServer(port=port) {
    val githubUser = createLocalGithubUser().copy(email = None)
    val code = "test"

    MockGithubData.addUser(githubUser, code)
    val user = await(
      anonClient.githubUsers.postGithub(GithubAuthenticationForm(code = code))
    )
    user.email should be(None)
    db.UsersDao.findByGithubUserId(githubUser.id).map(_.id) must beEqualTo(Some(user.id))
  }

}
