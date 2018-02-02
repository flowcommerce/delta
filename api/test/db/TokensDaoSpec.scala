package db

import io.flow.postgresql.Authorization
import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._
import java.util.UUID

class TokensDaoSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "setLatestByTag" in {
    val form = InternalTokenForm.UserCreated(createTokenForm())
    val token1 = rightOrErrors(TokensDao.create(systemUser, form))

    val token2 = TokensDao.setLatestByTag(systemUser, form)
    token1.id must not be(token2.id)
  }

  "findById" in {
    val token = createToken()
    TokensDao.findById(Authorization.All, token.id).map(_.id) must be(
      Some(token.id)
    )

    TokensDao.findById(Authorization.All, UUID.randomUUID.toString) must be(None)
  }

  "getCleartextGithubOauthTokenByUserId" in {
    val user = createUser()
    val actualToken = "foo"
    val form = InternalTokenForm.GithubOauth(user.id, actualToken)
    val token = TokensDao.create(systemUser, form)

    TokensDao.getCleartextGithubOauthTokenByUserId(user.id) must be(Some(actualToken))
    TokensDao.getCleartextGithubOauthTokenByUserId(createUser().id) must be(None)
  }

  "addCleartextIfAvailable" in {
    val token = createToken()
    token.cleartext must be(None)
    val clear = TokensDao.addCleartextIfAvailable(systemUser, token).cleartext.getOrElse {
      sys.error("Failed to read token")
    }
    token.masked must be(clear.substring(0, 3) + "-masked-xxx")
    TokensDao.addCleartextIfAvailable(systemUser, token).cleartext must be(None)
    clear.length must be(64)
  }

  "findAll by ids" in {
    val token1 = createToken()
    val token2 = createToken()

    TokensDao.findAll(Authorization.All, ids = Some(Seq(token1.id, token2.id))).map(_.id) must be(
      Seq(token1.id, token2.id)
    )

    TokensDao.findAll(Authorization.All, ids = Some(Nil)) must be(Nil)
    TokensDao.findAll(Authorization.All, ids = Some(Seq(UUID.randomUUID.toString))) must be(Nil)
    TokensDao.findAll(Authorization.All, ids = Some(Seq(token1.id, UUID.randomUUID.toString))).map(_.id) must be(Seq(token1.id))
  }

  "can only see own tokens" in {
    val user1 = createUser()
    val token1 = createToken(createTokenForm(user = user1))

    val user2 = createUser()
    val token2 = createToken(createTokenForm(user = user2))

    TokensDao.findAll(Authorization.User(user1.id)).map(_.id) must be(Seq(token1.id))
    TokensDao.findAll(Authorization.User(user2.id)).map(_.id) must be(Seq(token2.id))
    TokensDao.findAll(Authorization.User(createUser().id)).map(_.id) must be(Nil)

  }

}
