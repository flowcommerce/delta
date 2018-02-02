package db

import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.{DockerProvider, MembershipForm, Organization, OrganizationForm, Role}
import io.flow.postgresql.{Authorization, Query, Pager, OrderBy}
import io.flow.play.util.UrlKey
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object OrganizationsDao {

  private[this] val BaseQuery = Query(s"""
    select organizations.id,
           organizations.user_id,
           organizations.docker_provider,
           organizations.docker_organization,
           organizations.travis_organization,
           users.id as user_id,
           users.email as user_email,
           users.first_name as name_first,
           users.last_name as name_last
      from organizations
      join users on users.id = organizations.user_id
  """)

  def findById(auth: Authorization, id: String): Option[Organization] = {
    findAll(auth, id = Some(id), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    userId: Option[String] = None,
    orderBy: OrderBy = OrderBy("organizations.id"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[Organization] = {
    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "organizations",
        auth = Filters(auth).organizations("organizations.id"),
        id = id,
        ids = ids,
        orderBy = orderBy.sql,
        limit = limit,
        offset = offset
      ).
        and(
          userId.map { id =>
            "organizations.id in (select organization_id from memberships where user_id = {user_id})"
          }
        ).bind("user_id", userId).
        as(
          io.flow.delta.v0.anorm.parsers.Organization.parser().*
        )
    }

  }

}

case class OrganizationsWriteDao @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  projectsWriteDao: ProjectsWriteDao  
) {

  private[this] val InsertQuery = """
    insert into organizations
    (id, user_id, docker_provider, docker_organization, travis_organization, updated_by_user_id)
    values
    ({id}, {user_id}, {docker_provider}, {docker_organization}, {travis_organization}, {updated_by_user_id})
  """

  private[this] val UpdateQuery = """
    update organizations
       set updated_by_user_id = {updated_by_user_id},
           docker_provider = {docker_provider},
           docker_organization = {docker_organization},
           travis_organization = {travis_organization}
     where id = {id}
  """

  private[this] val urlKey = UrlKey(minKeyLength = 2)

  private[db] def validate(
    form: OrganizationForm,
    existing: Option[Organization] = None
  ): Seq[String] = {
    val idErrors = if (form.id.trim == "") {
      Seq("Id cannot be empty")

    } else {
      urlKey.validate(form.id.trim, "Id") match {
        case Nil => {
          OrganizationsDao.findById(Authorization.All, form.id) match {
            case None => Seq.empty
            case Some(p) => {
              Some(p.id) == existing.map(_.id) match {
                case true => Nil
                case false => Seq("Organization with this id already exists")
              }
            }
          }
        }
        case errors => errors
      }
    }

    val dockerProviderErrors = form.docker.provider match {
      case DockerProvider.UNDEFINED(_) => Seq("Docker provider not found")
      case _ => Nil
    }

    val dockerOrganizationErrors = form.docker.organization.trim match {
      case "" => Seq("Docker organization is required")
      case _ => Nil
    }

    idErrors ++ dockerProviderErrors ++ dockerOrganizationErrors
  }

  def create(createdBy: UserReference, form: OrganizationForm): Either[Seq[String], Organization] = {
    validate(form) match {
      case Nil => {
        val id = DB.withTransaction { implicit c =>
          create(c, createdBy, form)
        }

        Right(
          OrganizationsDao.findById(Authorization.All, id).getOrElse {
            sys.error("Failed to create organization")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  private[this] def create(implicit c: java.sql.Connection, createdBy: UserReference, form: OrganizationForm): String = {
    SQL(InsertQuery).on(
      'id -> form.id.trim,
      'user_id -> createdBy.id,
      'docker_provider -> form.docker.provider.toString,
      'docker_organization -> form.docker.organization.trim,
      'travis_organization -> form.travis.organization.trim,
      'updated_by_user_id -> createdBy.id
    ).execute()

    MembershipsDao.create(
      c,
      createdBy,
      form.id.trim,
      createdBy.id,
      Role.Admin
    )

    form.id.trim
  }

  def update(createdBy: UserReference, organization: Organization, form: OrganizationForm): Either[Seq[String], Organization] = {
    validate(form, Some(organization)) match {
      case Nil => {
        DB.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'id -> organization.id,
            'docker_provider -> form.docker.provider.toString,
            'docker_organization -> form.docker.organization.trim,
            'travis_organization -> form.travis.organization.trim,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        Right(
          OrganizationsDao.findById(Authorization.All, organization.id).getOrElse {
            sys.error("Failed to update organization")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def delete(deletedBy: UserReference, organization: Organization) {
    Pager.create { offset =>
      ProjectsDao.findAll(Authorization.All, organizationId = Some(organization.id), offset = offset)
    }.foreach { project =>
      projectsWriteDao.delete(deletedBy, project)
    }

    Pager.create { offset =>
      MembershipsDao.findAll(Authorization.All, organizationId = Some(organization.id), offset = offset)
    }.foreach { membership =>
      MembershipsDao.delete(deletedBy, membership)
    }

    Delete.delete("organizations", deletedBy.id, organization.id)
  }

}
