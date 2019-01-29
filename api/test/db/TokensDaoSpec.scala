package db

import java.util.UUID

import io.flow.postgresql.Authorization
import io.flow.test.utils.FlowPlaySpec

class TokensDaoSpec extends FlowPlaySpec with Helpers {

  "setLatestByTag" in {
    val form = InternalTokenForm.UserCreated(createTokenForm())
    val token1 = rightOrErrors(tokensDao.create(systemUser, form))

    val token2 = tokensDao.setLatestByTag(systemUser, form)
    token1.id must not be(token2.id)
  }

  "findById" in {
    val token = createToken()
    tokensDao.findById(Authorization.All, token.id).map(_.id) must be(
      Some(token.id)
    )

    tokensDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "getCleartextGithubOauthTokenByUserId" in {
    val user = createUser()
    val actualToken = "foo"
    val form = InternalTokenForm.GithubOauth(user.id, actualToken)
    tokensDao.create(systemUser, form)

    tokensDao.getCleartextGithubOauthTokenByUserId(user.id) must be(Some(actualToken))
    tokensDao.getCleartextGithubOauthTokenByUserId(createUser().id) must be(None)
  }

  "addCleartextIfAvailable" in {
    val token = createToken()
    token.cleartext must be(None)
    val clear = tokensDao.addCleartextIfAvailable(systemUser, token).cleartext.getOrElse {
      sys.error("Failed to read token")
    }
    token.masked must be(clear.substring(0, 3) + "-masked-xxx")
    tokensDao.addCleartextIfAvailable(systemUser, token).cleartext must be(None)
    clear.length must be(64)
  }

  "findAll by ids" in {
    val token1 = createToken()
    val token2 = createToken()

    tokensDao.findAll(Authorization.All, ids = Some(Seq(token1.id, token2.id)), limit = None).map(_.id) must be(
      Seq(token1.id, token2.id)
    )

    tokensDao.findAll(Authorization.All, ids = Some(Nil), limit = None) must be(Nil)
    tokensDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString)), limit = None) must be(Nil)
    tokensDao.findAll(Authorization.All, ids = Some(Seq(token1.id, UUID.randomUUID.toString)), limit = None).map(_.id) must be(Seq(token1.id))
  }

  "can only see own tokens" in {
    val user1 = createUserReference()
    val token1 = createToken(createTokenForm(user = user1))

    val user2 = createUserReference()
    val token2 = createToken(createTokenForm(user = user2))

    tokensDao.findAll(Authorization.User(user1.id), limit = None).map(_.id) must be(Seq(token1.id))
    tokensDao.findAll(Authorization.User(user2.id), limit = None).map(_.id) must be(Seq(token2.id))
    tokensDao.findAll(Authorization.User(createUser().id), limit = None).map(_.id) must be(Nil)

  }

}
