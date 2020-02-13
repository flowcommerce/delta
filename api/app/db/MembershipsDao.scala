package db

import anorm._
import com.google.inject.Provider
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.{Membership, MembershipForm, Role}
import io.flow.postgresql.{Authorization, OrderBy, Query}
import io.flow.util.IdGenerator
import play.api.db._

@javax.inject.Singleton
class MembershipsDao @javax.inject.Inject() (
  db: Database,
  delete: Delete,
  membershipsDao: Provider[MembershipsDao],
  organizationsDao: Provider[OrganizationsDao]
) {

  val DefaultUserNameLength = 8

  private[this] val BaseQuery = Query(s"""
    select memberships.id,
           memberships.role,
           organizations.id as organization_id,
           users.id as user_id,
           users.email as user_email,
           users.first_name as user_name_first,
           users.last_name as user_name_last
      from memberships
      join organizations on organizations.id = memberships.organization_id
      join users on users.id = memberships.user_id
  """)

  private[this] val InsertQuery = """
    insert into memberships
    (id, role, user_id, organization_id, updated_by_user_id)
    values
    ({id}, {role}, {user_id}, {organization_id}, {updated_by_user_id})
  """

  def isMember(orgId: String, user: UserReference): Boolean = {
    membershipsDao.get().findByOrganizationIdAndUserId(Authorization.All, orgId, user.id) match {
      case None => false
      case Some(_) => true
    }
  }

  private[db] def validate(
    user: UserReference,
    form: MembershipForm
  ): Seq[String] = {
    val roleErrors = form.role match {
      case Role.UNDEFINED(_) => Seq("Invalid role. Must be one of: " + Role.all.map(_.toString).mkString(", "))
      case _ => {
        membershipsDao.get().findByOrganizationIdAndUserId(Authorization.All, form.organization, form.userId) match {
          case None => Seq.empty
          case Some(_) => Seq("User is already a member")
        }
      }
    }

    val organizationErrors = membershipsDao.get().findByOrganizationIdAndUserId(Authorization.All, form.organization, user.id) match {
      case None => Seq("Organization does not exist or you are not authorized to access this organization")
      case Some(_) => Nil
    }

    roleErrors ++ organizationErrors
  }

  def create(createdBy: UserReference, form: MembershipForm): Either[Seq[String], Membership] = {
    validate(createdBy, form) match {
      case Nil => {
        val id = db.withConnection { implicit c =>
          create(c, createdBy, form)
        }
        Right(
          findById(Authorization.All, id).getOrElse {
            sys.error("Failed to create membership")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: UserReference, form: MembershipForm): String = {
    val org = organizationsDao.get().findById(Authorization.All, form.organization).getOrElse {
      sys.error(s"Could not find organization with id[${form.organization}]")
    }

    create(c, createdBy, org.id, form.userId, form.role)
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: UserReference, orgId: String, userId: String, role: Role): String = {
    val id = IdGenerator("mem").randomId()

    SQL(InsertQuery).on(
      Symbol("id") ->id,
      Symbol("user_id") ->userId,
      Symbol("organization_id") ->orgId,
      Symbol("role") ->role.toString,
      Symbol("updated_by_user_id") ->createdBy.id
    ).execute()
    id
  }

  def delete(deletedBy: UserReference, membership: Membership): Unit = {
    delete.delete("memberships", deletedBy.id, membership.id)
  }

  def findByOrganizationIdAndUserId(
    auth: Authorization,
    organizationId: String,
    userId: String
  ): Option[Membership] = {
    findAll(
      auth,
      organizationId = Some(organizationId),
      userId = Some(userId),
      limit = Some(1)
    ).headOption
  }

  def findById(auth: Authorization, id: String): Option[Membership] = {
    findAll(auth, id = Some(id), limit = Some(1)).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    organizationId: Option[String] = None,
    userId: Option[String] = None,
    role: Option[Role] = None,
    orderBy: OrderBy = OrderBy("memberships.created_at"),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[Membership] = {
    db.withConnection { implicit c =>
    Standards.query(
      BaseQuery,
      tableName = "memberships",
      auth = Filters(auth).organizations("organizations.id"),
      id = id,
      ids = ids,
      orderBy = orderBy.sql,
      limit = limit,
      offset = offset
    ).
      equals("memberships.organization_id", organizationId).
      equals("memberships.user_id", userId).
      optionalText("memberships.role", role.map(_.toString.toLowerCase)).
      as(
        io.flow.delta.v0.anorm.parsers.Membership.parser().*
      )
    }
  }

}
