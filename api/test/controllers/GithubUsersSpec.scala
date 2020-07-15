package controllers

import io.flow.delta.api.lib.MockGithubData
import io.flow.delta.v0.errors.GenericErrorResponse
import io.flow.delta.v0.models.GithubAuthenticationForm
import io.flow.github.v0.models.{OwnerType, User => GithubUser}

class GithubUsersSpec extends MockClient with db.Helpers {
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
      `type` = OwnerType.User,
    )
  }

  "POST /authentications/github with valid token" in {
    val githubUser = createLocalGithubUser()
    val code = "test"

    MockGithubData.addUser(githubUser, code, organizations = Seq("flowcommerce"))

    val user = await(anonClient.githubUsers.postGithub(GithubAuthenticationForm(code = code)))

    user.email must be(githubUser.email)

    githubUsersDao.findAll(userId = Some(user.id), limit = Some(1)).headOption.map(_.user.id) must be(Some(user.id))

    // Test idempotence
    val user2 = await(anonClient.githubUsers.postGithub(GithubAuthenticationForm(code = code)))
    user2.email must be(githubUser.email)
  }

  "POST /authentications/github accepts account w/out email" in {
    val githubUser = createLocalGithubUser().copy(email = None)
    val code = "test"

    MockGithubData.addUser(githubUser, code, organizations = Seq("flowcommerce"))
    val user = await(
      anonClient.githubUsers.postGithub(GithubAuthenticationForm(code = code))
    )
    user.email must be(None)
    usersDao.findByGithubUserId(githubUser.id).map(_.id) must be(Some(user.id))
  }

  "POST /authentication/github should deny accounts that are not in the required orgs" in {
    val githubUser = createLocalGithubUser().copy(email = None)
    val code = "test"

    MockGithubData.addUser(githubUser, code, organizations = Seq("fbi", "nsa", "spacex"))
    a [GenericErrorResponse] must be thrownBy await(
      anonClient.githubUsers.postGithub(GithubAuthenticationForm(code = code))
    )
  }

}
