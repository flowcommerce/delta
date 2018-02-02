package db

import io.flow.delta.v0.models.UserForm
import io.flow.delta.actors.MainActor
import io.flow.postgresql.{Authorization, Query, OrderBy}
import io.flow.common.v0.models.{Name, User, UserReference}
import anorm._
import play.api.db._
import play.api.Play.current

object UsersDao {

  private[db] val SystemEmailAddress = "otto@flow.io"
  private[db] val AnonymousEmailAddress = "anonymous@flow.io"

  lazy val systemUser = io.flow.play.util.Constants.SystemUser

  lazy val anonymousUser = io.flow.play.util.Constants.AnonymousUser

  private[this] val BaseQuery = Query(s"""
    select users.id,
           users.email,
           users.first_name as name_first,
           users.last_name as name_last
      from users
  """)


  def findByGithubUserId(githubUserId: Long): Option[User] = {
    findAll(githubUserId = Some(githubUserId), limit = 1).headOption
  }

  def findByEmail(email: String): Option[User] = {
    findAll(email = Some(email), limit = 1).headOption
  }

  def findByToken(token: String): Option[User] = {
    findAll(token = Some(token), limit = 1).headOption
  }

  def findById(id: String): Option[User] = {
    findAll(id = Some(id), limit = 1).headOption
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    email: Option[String] = None,
    token: Option[String] = None,
    identifier: Option[String] = None,
    githubUserId: Option[Long] = None,
    orderBy: OrderBy = OrderBy("users.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[User] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "users",
        auth = Clause.True, // TODO
        id = id,
        ids = ids,
        orderBy = orderBy.sql,
        limit = limit,
        offset = offset
      ).
        optionalText(
          "users.email",
          email,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        and(
          identifier.map { id =>
            "users.id in (select user_id from user_identifiers where value = trim({identifier}))"
          }
        ).bind("identifier", identifier).
        and(
          token.map { t =>
            "users.id in (select user_id from tokens where token = trim({token}))"
          }
        ).bind("token", token).
        and(
          githubUserId.map { id =>
            "users.id in (select user_id from github_users where github_user_id = {github_user_id}::numeric)"
          }
        ).bind("github_user_id", githubUserId).
        as(
          io.flow.common.v0.anorm.parsers.User.parser().*
        )
    }
  }
}

case class UsersWriteDao @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef
) {

  private[this] val InsertQuery = """
    insert into users
    (id, email, first_name, last_name, updated_by_user_id)
    values
    ({id}, {email}, {first_name}, {last_name}, {updated_by_user_id})
  """

  def validate(form: UserForm): Seq[String] = {
    form.email match {
      case None => {
        Nil
      }
      case Some(email) => {
        if (email.trim.isEmpty) {
          Seq("Email address cannot be empty")

        } else if (!isValidEmail(email)) {
          Seq("Please enter a valid email address")

        } else {
          UsersDao.findByEmail(email) match {
            case None => Nil
            case Some(_) => Seq("Email is already registered")
          }
        }
      }
    }
  }

  private def isValidEmail(email: String): Boolean = {
    email.indexOf("@") >= 0
  }

  def create(createdBy: Option[UserReference], form: UserForm): Either[Seq[String], User] = {
    validate(form) match {
      case Nil => {
        val id = io.flow.play.util.IdGenerator("usr").randomId()

        DB.withConnection { implicit c =>
          SQL(InsertQuery).on(
            'id -> id,
            'email -> form.email.map(_.trim),
            'first_name -> Util.trimmedString(form.name.flatMap(_.first)),
            'last_name -> Util.trimmedString(form.name.flatMap(_.last)),
            'updated_by_user_id -> createdBy.getOrElse(UsersDao.anonymousUser).id
          ).execute()
        }

        mainActor ! MainActor.Messages.UserCreated(id.toString)

        Right(
          UsersDao.findById(id).getOrElse {
            sys.error("Failed to create user")
          }
        )
      }
      case errors => Left(errors)
    }
  }

}

