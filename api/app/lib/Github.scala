package io.flow.delta.api.lib

import javax.inject.Inject
import db._
import io.flow.common.v0.models.{Name, User, UserReference}
import io.flow.delta.v0.models.{GithubUserForm, UserForm}
import io.flow.github.oauth.v0.models.AccessTokenForm
import io.flow.github.oauth.v0.{Client => GithubOauthClient}
import io.flow.github.v0.errors.UnitResponse
import io.flow.github.v0.models.{Contents, Encoding, Repository => GithubRepository, User => GithubUser}
import io.flow.github.v0.{Client => GithubClient}
import io.flow.log.RollbarLogger
import io.flow.play.util.{Config, IdGenerator}
import org.apache.commons.codec.binary.Base64
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

case class GithubUserData(
  githubId: Long,
  login: String,
  token: String,
  emails: Seq[String],
  name: Option[String],
  avatarUrl: Option[String]
)

class GitHubHelper @javax.inject.Inject() (
  tokensDao: TokensDao,
  usersDao: UsersDao,
  wsClient: WSClient,
  logger: RollbarLogger
) {


  /**
    * Looks up this user's oauth token, and if found, returns an instance of the github client.
    */
  def apiClientFromUser(userId: String): Option[GithubClient] = {
    usersDao.findById(userId).flatMap { u =>
      tokensDao.getCleartextGithubOauthTokenByUserId(u.id)
    } match {
      case None => {
        logger.withKeyValue("user_id", userId).warn("No Github oauth token for user")
        None
      }
      case Some(token) => {
        Some(apiClient(token))
      }
    }
  }

  def apiClient(oauthToken: String): GithubClient = {
    new GithubClient(
      ws = wsClient,
      baseUrl = "https://api.github.com",
      defaultHeaders = Seq(
        "Authorization" -> s"token $oauthToken"
      )
    )
  }

  def parseName(value: String): Name = {
    if (value.trim.isEmpty) {
      Name()
    } else {
      value.trim.split("\\s+").toList match {
        case Nil => Name()
        case first :: Nil => Name(first = Some(first))
        case first :: last :: Nil => Name(first = Some(first), last = Some(last))
        case first :: multiple => Name(first = Some(first), last = Some(multiple.mkString(" ")))
      }
    }
  }

}

trait Github {

  private[this] val DotDeltaPath = ".delta"

  /**
    * Given an auth validation code, pings the github UI to access the
    * user data, upserts that user with the delta database, and
    * returns the user (or a list of errors).
    *
    * @param code The oauth authorization code from github
    */
  def getUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], User]]

  /**
    * Fetches github user from an oauth code
    */
  def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserData]]

  /**
    * Fetches one page of repositories from the Github API
    */
  def githubRepos(user: UserReference, page: Long = 1)(implicit ec: ExecutionContext): Future[Seq[GithubRepository]]

  /**
    * Fetches the specified file, if it exists, from this repo
    */
  def file(
    user: UserReference, owner: String, repo: String, path: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[String]]

  /**
    * Fetches the specified file, if it exists, from this repo
    */
  def dotDeltaFile(
    user: UserReference, owner: String, repo: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[String]] = file(user, owner, repo, DotDeltaPath)

  /**
    * Recursively calls the github API until we either:
    *  - consume all records
    *  - meet the specified limit/offset
    */
  def repositories(
    user: UserReference,
    offset: Long,
    limit: Long,
    resultsSoFar: Seq[GithubRepository] = Nil,
    page: Long = 1  // internal parameter
  ) (
    acceptsFilter: GithubRepository => Boolean = { _ => true }
  ) (
    implicit ec: ExecutionContext
  ): Future[Seq[GithubRepository]] = {
    githubRepos(user, page).flatMap { thisPage =>
      if (thisPage.isEmpty) {
        Future {
          resultsSoFar.drop(offset.toInt).take(limit.toInt)
        }
      } else {
        val all = resultsSoFar ++ thisPage.filter { acceptsFilter(_) }
        if (all.size >= offset + limit) {
          Future {
            all.drop(offset.toInt).take(limit.toInt)
          }
        } else {
          repositories(user, offset, limit, all, page + 1)(acceptsFilter)
        }
      }
    }
  }

  /**
    * For this user, returns the oauth token if available
    */
  def oauthToken(user: UserReference): Option[String]

}

@javax.inject.Singleton
class DefaultGithub @javax.inject.Inject() (
  logger: RollbarLogger,
  config: Config,
  gitHubHelper: GitHubHelper,
  githubUsersDao: GithubUsersDao,
  tokensDao: TokensDao,
  usersDao: UsersDao,
  usersWriteDao: UsersWriteDao,
  wSClient: WSClient
) extends Github {

  private[this] lazy val clientId = config.requiredString("github.delta.client.id")
  private[this] lazy val clientSecret = config.requiredString("github.delta.client.secret")

  private[this] lazy val oauthClient = new GithubOauthClient(
    wSClient,
    baseUrl = "https://github.com",
    defaultHeaders = Seq(
      "Accept" -> "application/json"
    )
  )

  override def getUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], User]] = {
    getGithubUserFromCode(code).map {
      case Left(errors) => Left(errors)
      case Right(githubUserWithToken) => {
        val userResult: Either[Seq[String], User] = usersDao.findByGithubUserId(githubUserWithToken.githubId) match {
          case Some(user) => {
            Right(user)
          }
          case None => {
            githubUserWithToken.emails.headOption flatMap { email =>
              usersDao.findByEmail(email)
            } match {
              case Some(user) => {
                Right(user)
              }
              case None => {
                usersWriteDao.create(
                  createdBy = None,
                  form = UserForm(
                    email = githubUserWithToken.emails.headOption,
                    name = githubUserWithToken.name.map(gitHubHelper.parseName)
                  )
                )
              }
            }
          }
        }

        userResult match {
          case Left(errors) => {
            Left(errors)
          }
          case Right(user) => {
            githubUsersDao.upsertById(
              createdBy = None,
              form = GithubUserForm(
                userId = user.id,
                githubUserId = githubUserWithToken.githubId,
                login = githubUserWithToken.login
              )
            )

            tokensDao.setLatestByTag(
              createdBy = UserReference(id = user.id),
              form = InternalTokenForm.GithubOauth(
                userId = user.id,
                token = githubUserWithToken.token
              )
            )

            Right(user)
          }
        }
      }
    }
  }

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserData]] = {
    oauthClient.accessTokens.postAccessToken(
      AccessTokenForm(
        clientId = clientId,
        clientSecret = clientSecret,
        code = code
      )
    ).flatMap { response =>
      val client = gitHubHelper.apiClient(response.accessToken)
      for {
        githubUser <- client.users.getUser()
        emails <- client.userEmails.get()
      } yield {
        // put primary first
        val sortedEmailAddresses = (emails.filter(_.primary) ++ emails.filter(!_.primary)).map(_.email)

        Right(
          GithubUserData(
            githubId = githubUser.id,
            login = githubUser.login,
            token = response.accessToken,
            emails = sortedEmailAddresses,
            name = githubUser.name,
            avatarUrl = githubUser.avatarUrl
          )
        )
      }
    }.recover {
      case ex: Throwable => {
        logger.warn("Failed to post access token to Github OAuth client", ex)
        throw new RuntimeException(ex)
      }
    }
  }

  override def githubRepos(user: UserReference, page: Long = 1)(implicit ec: ExecutionContext): Future[Seq[GithubRepository]] = {
    oauthToken(user) match {
      case None => Future { Nil }
      case Some(token) => {
        gitHubHelper.apiClient(token).repositories.getUserAndRepos(page)
      }
    }
  }

  override def file(
    user: UserReference, owner: String, repo: String, path: String
  ) (
    implicit ec: ExecutionContext
  ): Future[Option[String]] = {
    oauthToken(user) match {
      case None => Future { None }
      case Some(token) => {
        gitHubHelper.apiClient(token).contents.getContentsByPath(
          owner = owner,
          repo = repo,
          path = path
        ).map { contents =>
          Some(toText(contents))
        }.recover {
          case UnitResponse(404) => {
            None
          }
        }
      }
    }
  }

  private[this] def toText(contents: Contents): String = {
    (contents.content, contents.encoding) match {
      case (Some(encoded), Encoding.Base64) => {
        new String(Base64.decodeBase64(encoded.getBytes))
      }
      case (Some(encoded), Encoding.Utf8) => {
        encoded
      }
      case (Some(_), Encoding.UNDEFINED(name)) => {
        sys.error(s"Unsupported encoding[$name] for content: $contents")
      }
      case (None, _) => {
        sys.error(s"No contents for: $contents")
      }
    }
  }

  override def oauthToken(user: UserReference): Option[String] = {
    tokensDao.getCleartextGithubOauthTokenByUserId(user.id)
  }

}

class MockGithub @Inject()(
  gitHubHelper: GitHubHelper,
  githubUsersDao: GithubUsersDao,
  tokensDao: TokensDao,
  usersDao: UsersDao,
  usersWriteDao: UsersWriteDao
) extends Github {

  override def getUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], User]] = {
    getGithubUserFromCode(code).map {
      case Left(errors) => Left(errors)
      case Right(githubUserWithToken) => {
        val userResult: Either[Seq[String], User] = usersDao.findByGithubUserId(githubUserWithToken.githubId) match {
          case Some(user) => {
            Right(user)
          }
          case None => {
            githubUserWithToken.emails.headOption flatMap { email =>
              usersDao.findByEmail(email)
            } match {
              case Some(user) => {
                Right(user)
              }
              case None => {
                usersWriteDao.create(
                  createdBy = None,
                  form = UserForm(
                    email = githubUserWithToken.emails.headOption,
                    name = githubUserWithToken.name.map(gitHubHelper.parseName)
                  )
                )
              }
            }
          }
        }

        userResult match {
          case Left(errors) => {
            Left(errors)
          }
          case Right(user) => {
            githubUsersDao.upsertById(
              createdBy = None,
              form = GithubUserForm(
                userId = user.id,
                githubUserId = githubUserWithToken.githubId,
                login = githubUserWithToken.login
              )
            )

            tokensDao.setLatestByTag(
              createdBy = UserReference(id = user.id),
              form = InternalTokenForm.GithubOauth(
                userId = user.id,
                token = githubUserWithToken.token
              )
            )

            Right(user)
          }
        }
      }
    }
  }

  override def getGithubUserFromCode(code: String)(implicit ec: ExecutionContext): Future[Either[Seq[String], GithubUserData]] = {
    Future {
      MockGithubData.getUserByCode(code) match {
        case None => Left(Seq("Invalid access code"))
        case Some(u) => Right(u)
      }
    }
  }

  override def githubRepos(user: UserReference, page: Long = 1)(implicit ec: ExecutionContext): Future[Seq[GithubRepository]] = {
    Future {
      MockGithubData.repositories(user)
    }
  }

  override def file(
    user: UserReference, owner: String, repo: String, path: String
  ) (
    implicit ec: ExecutionContext
  ) = Future {
    MockGithubData.getFile(repo, path)
  }
  
  override def oauthToken(user: UserReference): Option[String] = {
    MockGithubData.getToken(user)
  }

}

object MockGithubData {
  private[this] var githubUserByCodes = scala.collection.mutable.Map[String, GithubUserData]()
  private[this] var userTokens = scala.collection.mutable.Map[String, String]()
  private[this] var repositories = scala.collection.mutable.Map[String, GithubRepository]()
  private[this] var files = scala.collection.mutable.Map[String, String]()

  def addUser(githubUser: GithubUser, code: String, token: Option[String] = None) {
    githubUserByCodes +== (
      code -> GithubUserData(
        githubId = githubUser.id,
        login = githubUser.login,
        token = token.getOrElse(IdGenerator("tok").randomId),
        emails = Seq(githubUser.email).flatten,
        name = githubUser.name,
        avatarUrl = githubUser.avatarUrl
      )
    )
  }

  def getUserByCode(code: String): Option[GithubUserData] = {
    githubUserByCodes.get(code)
  }

  def addUserOauthToken(token: String, user: UserReference) {
    userTokens +== (user.id -> token)
  }

  def getToken(user: UserReference): Option[String] = {
    userTokens.get(user.id)
  }

  def addRepository(user: UserReference, repository: GithubRepository): Unit = {
    repositories +== (user.id -> repository)
  }

  def repositories(user: UserReference): Seq[GithubRepository] = {
    repositories.get(user.id).toSeq
  }

  def addFile(repo: String, path: String, contents: String): Unit = {
    files += (s"repo:$path" -> contents)
  }

  def getFile(repo: String, path: String): Option[String] = {
    files.get(s"repo:$path")
  }
}
