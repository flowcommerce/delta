package db

import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.{Publication, Subscription, SubscriptionForm}
import io.flow.postgresql.{OrderBy, Query}
import io.flow.util.IdGenerator
import play.api.db._

import scala.util.{Failure, Success, Try}

@javax.inject.Singleton
class SubscriptionsDao @javax.inject.Inject() (
  db: Database,
  usersDao: UsersDao,
  delete: Delete
) {

  private[this] val BaseQuery = Query(s"""
    select subscriptions.id,
           subscriptions.user_id,
           subscriptions.publication
      from subscriptions
  """)

  private[this] val InsertQuery = """
    insert into subscriptions
    (id, user_id, publication, updated_by_user_id)
    values
    ({id}, {user_id}, {publication}, {updated_by_user_id})
  """

  private[db] def validate(
    form: SubscriptionForm
  ): Seq[String] = {
    val userErrors = usersDao.findById(form.userId) match {
      case None => Seq("User not found")
      case Some(_) => Nil
    }

    val publicationErrors = form.publication match {
      case Publication.UNDEFINED(_) => Seq("Invalid publication")
      case _ => Nil
    }

    userErrors ++ publicationErrors
  }

  def upsert(createdBy: UserReference, form: SubscriptionForm): Subscription = {
    findByUserIdAndPublication(form.userId, form.publication).getOrElse {
      Try {
        create(createdBy, form) match {
          case Left(errors) => sys.error(errors.mkString(", "))
          case Right(sub) => sub
        }
      } match {
        case Success(sub) => sub
        case Failure(ex) => {
          findByUserIdAndPublication(form.userId, form.publication).getOrElse {
            throw new Exception("Failed to upsert subscription", ex)
          }
        }
      }
    }
  }

  def create(createdBy: UserReference, form: SubscriptionForm): Either[Seq[String], Subscription] = {
    validate(form) match {
      case Nil => {
        val id = IdGenerator("sub").randomId()

        db.withConnection { implicit c =>
          SQL(InsertQuery).on(
            Symbol("id") ->id,
            Symbol("user_id") ->form.userId,
            Symbol("publication") ->form.publication.toString,
            Symbol("updated_by_user_id") ->createdBy.id
          ).execute()
        }

        Right(
          findById(id).getOrElse {
            sys.error("Failed to create subscription")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def delete(deletedBy: UserReference, subscription: Subscription): Unit = {
    delete.delete("subscriptions", deletedBy.id, subscription.id)
  }

  def findByUserIdAndPublication(
    userId: String,
    publication: Publication
  ): Option[Subscription] = {
    findAll(
      userId = Some(userId),
      publication = Some(publication),
      limit = Some(1)
    ).headOption
  }

  def findById(id: String): Option[Subscription] = {
    findAll(id = Some(id), limit = Some(1)).headOption
  }

  def findAll(
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userId: Option[String] = None,
    identifier: Option[String] = None,
    publication: Option[Publication] = None,
    orderBy: OrderBy = OrderBy("subscriptions.created_at"),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[Subscription] = {
    db.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "subscriptions",
        auth = Clause.True, // TODO
        id = id,
        ids = ids,
        orderBy = orderBy.sql,
        limit = limit,
        offset = offset
      ).
        equals("subscriptions.user_id", userId).
        optionalText("subscriptions.publication", publication).
        and(
          identifier.map { _ =>
            "subscriptions.user_id in (select user_id from user_identifiers where value = trim({identifier}))"
          }
        ).bind("identifier", identifier).
        as(
          io.flow.delta.v0.anorm.parsers.Subscription.parser().*
        )
    }
  }

}
