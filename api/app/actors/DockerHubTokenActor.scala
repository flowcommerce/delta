package io.flow.delta.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem}
import db.{OrganizationsDao, UsersDao, VariablesDao}
import io.flow.delta.v0.models.VariableForm
import io.flow.docker.hub.v0.Client
import io.flow.docker.hub.v0.models.{Jwt, JwtForm}
import io.flow.log.RollbarLogger
import io.flow.play.actors.ErrorHandler
import io.flow.play.util.Config
import io.flow.postgresql.Authorization
import play.api.libs.ws.WSClient

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@javax.inject.Singleton
class DockerHubToken @javax.inject.Inject() (
  config: Config,
  organizationsDao: OrganizationsDao,
  usersDao: UsersDao,
  variablesDao: VariablesDao,
  wSClient: WSClient,
  logger: RollbarLogger,
  implicit val ec: ExecutionContext
) {

  private[this] val tokenKey = "DOCKER_JWT_TOKEN"
  private[this] val jwtClient = new Client(ws = wSClient)
  private[this] val auth = Authorization.All

  /**
    * For each organization, check if token is in db
    * If yes, then load to memory
    * If no, then create one and wait
    */
  private[this] lazy val orgTokenMap = {
    val map = scala.collection.mutable.HashMap.empty[String, String]
    organizationsDao.findAll(auth = auth).foreach { organization =>
      val token = variablesDao.findByOrganizationAndKey(auth = auth, organization = organization.id, key = tokenKey) match {
        case None => {
          val jwt = generate
          val form = VariableForm(organization.id, tokenKey, jwt)
          variablesDao.upsert(Authorization.All, usersDao.systemUser, form)
          jwt
        }
        case Some(variable) => variable.value
      }
      map += (organization.id -> token)
    }
    map
  }

  /**
    * Get token given an organization
    */
  def get(organization: String): String = {
    orgTokenMap.contains(organization) match {
      case true => orgTokenMap(organization)
      case false => {
        val token = generate
        orgTokenMap += (organization -> token)
        token
      }
    }
  }

  /**
    * Blocking call to create a new token
    */
  def generate(): String = {
    Try(
      Await.result(generateTokenFuture(), Duration(10, TimeUnit.SECONDS))
    ) match {
      case Success(response) => response.token
      case Failure(ex) => sys.error(s"Got an Exception: ${ex.getMessage}")
    }
  }

  /**
    * Function called when periodically trying to refresh
    * Should iterate through organizations and update their tokens
    */
  def refresh()(implicit ec: ExecutionContext) {
    organizationsDao.findAll(Authorization.All).foreach { organization =>
      generateTokenFuture.map { jwt =>
        val form = VariableForm(organization.id, tokenKey, jwt.token)
        variablesDao.upsert(Authorization.All, usersDao.systemUser, form) match {
          case Left(errors) => logger.withKeyValue("errors", errors).error(s"Error refreshing docker hub JWT token")
          case Right(variable) => {
            orgTokenMap += (organization.id -> variable.value)
          }
        }
      }.recover {
        case ex: Throwable => logger.error(s"Error refreshing docker hub JWT token", ex)
      }
    }
  }

  private[this] def generateTokenFuture()(implicit ec: ExecutionContext): Future[Jwt] = {
    // TODO: Docker username/password (and other configs) go into DB
    val username = config.requiredString("docker.username")
    val password = config.requiredString("docker.password")
    val form = JwtForm(username = username, password = password)
    jwtClient.jwts.postLogin(form)
  }

  def requestHeaders(organization: String) = {
    Seq(
      ("Authorization", s"Bearer ${get(organization)}")
    )
  }
}

object DockerHubTokenActor {
  object Messages {
    case object Refresh
  }

  trait Factory {
    def apply(): Actor
  }
}

class DockerHubTokenActor @javax.inject.Inject() (
  override val logger: RollbarLogger,
  system: ActorSystem,
  dockerHubToken: DockerHubToken
) extends Actor with ErrorHandler {

  private[this] implicit val ec = system.dispatchers.lookup("dockerhub-actor-context")

  def receive = {

    case msg @ DockerHubTokenActor.Messages.Refresh => withErrorHandler(msg) {
      dockerHubToken.refresh()
    }

  }

}
