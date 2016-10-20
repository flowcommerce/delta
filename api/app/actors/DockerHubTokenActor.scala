package io.flow.delta.actors

import io.flow.postgresql.Authorization
import java.util.concurrent.TimeUnit

import io.flow.docker.hub.v0.Client
import io.flow.docker.hub.v0.models.{Jwt, JwtForm}
import io.flow.play.actors.ErrorHandler
import io.flow.play.util.Config
import akka.actor.{Actor, ActorSystem}
import io.flow.delta.v0.models.{Variable, VariableForm}
import play.api.Logger

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

@javax.inject.Singleton
class DockerHubToken @javax.inject.Inject() (
  config: Config
) {

  private[this] val tokenKey = "DOCKER_JWT_TOKEN"
  private[this] val jwtClient = new Client()
  private[this] val auth = Authorization.All

  /**
    * For each organization, check if token is in db
    * If yes, then load to memory
    * If no, then create one and wait
    */
  private[this] lazy val orgTokenMap = {
    val map = scala.collection.mutable.HashMap.empty[String, String]
    db.OrganizationsDao.findAll(auth = auth).foreach { organization =>
      val token = db.VariablesDao.findByOrganizationAndKey(auth = auth, organization = organization.id, key = tokenKey) match {
        case None => {
          val jwt = generate
          val form = VariableForm(organization.id, tokenKey, jwt)
          db.VariablesDao.upsert(Authorization.All, db.UsersDao.systemUser, form)
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
    import scala.concurrent.ExecutionContext.Implicits.global
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
    db.OrganizationsDao.findAll(Authorization.All).foreach { organization =>
      generateTokenFuture.map { jwt =>
        val form = VariableForm(organization.id, tokenKey, jwt.token)
        db.VariablesDao.upsert(Authorization.All, db.UsersDao.systemUser, form) match {
          case Left(errors) => Logger.error(s"Error refreshing docker hub JWT token: $errors")
          case Right(variable) => {
            orgTokenMap += (organization.id -> variable.value)
          }
        }
      }.recover {
        case ex: Throwable => Logger.error(s"Error refreshing docker hub JWT token: ${ex.getMessage}")
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
