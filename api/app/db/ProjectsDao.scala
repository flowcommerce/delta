package db

import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.actors.MainActor
import io.flow.delta.api.lib.GithubUtil
import io.flow.delta.config.v0.models.Config
import io.flow.delta.config.v0.models.json._
import io.flow.delta.lib.config.Defaults
import io.flow.delta.v0.models._
import io.flow.play.util.UrlKey
import io.flow.postgresql.{Authorization, OrderBy, Pager, Query}
import play.api.db._
import play.api.libs.json._

@javax.inject.Singleton
class ProjectsDao @javax.inject.Inject() (
  db: Database
) {

  private[this] val BaseQuery = Query(s"""
    select projects.id,
           projects.user_id,
           projects.visibility,
           projects.scms,
           projects.name,
           projects.uri,
           organizations.id as organization_id,
           configs.data::varchar as config_data
      from projects
      left join organizations on organizations.id = projects.organization_id
      left join configs on configs.project_id = projects.id
  """)
  
  def findByOrganizationIdAndName(auth: Authorization, organizationId: String, name: String): Option[Project] = {
    findAll(auth, organizationId = Some(organizationId), name = Some(name), limit = Some(1)).headOption
  }

  def findById(auth: Authorization, id: String): Option[Project] = {
    findAll(auth, id = Some(id), limit = Some(1)).headOption
  }

  def findByBuildId(auth: Authorization, buildId: String): Option[Project] = {
    findAll(auth, buildId = Some(buildId), limit = Some(1)).headOption
  }

  def findAll(
    auth: Authorization,
    id: Option[String] = None,
    ids: Option[Seq[String]] = None,
    buildId: Option[String] = None,
    organizationId: Option[String] = None,
    name: Option[String] = None,
    minutesSinceLastEvent: Option[Long] = None,
    orderBy: OrderBy = OrderBy("lower(projects.name), projects.created_at"),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[Project] = {

    db.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "projects",
        auth = Filters(auth).organizations("projects.organization_id", Some("projects.visibility")),
        id = id,
        ids = ids,
        orderBy = orderBy.sql,
        limit = limit,
        offset = offset
      ).
        optionalText(
          "organizations.id",
          organizationId,
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        optionalText(
          "projects.name",
          name,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        and(
          buildId.map { _ =>
            "id = (select project_id from builds where id = {build_id})"
          }
        ).bind("build_id", buildId).
        and(
          minutesSinceLastEvent.map { _ =>
            "not exists (select 1 from events where events.project_id = projects.id and events.created_at > now() - interval '1 minute' * {minutes_since_last_event}::bigint)"
          }
        ).bind("minutes_since_last_event", minutesSinceLastEvent).
        as(
          parser().*
        )
    }
  }

  private[this] def parser(): RowParser[Project] = {
    SqlParser.str("id") ~
    io.flow.delta.v0.anorm.parsers.OrganizationSummary.parserWithPrefix("organization") ~
    io.flow.delta.v0.anorm.parsers.Reference.parserWithPrefix("user") ~
    io.flow.delta.v0.anorm.parsers.Visibility.parser("visibility") ~
    io.flow.delta.v0.anorm.parsers.Scms.parser("scms") ~
    SqlParser.str("name") ~
    SqlParser.str("uri") ~
    SqlParser.str("config_data").? map {
      case id ~ organization ~ user ~ visibility ~ scms ~ name ~ uri ~ configData => {
        Project(
          id = id,
          organization = organization,
          user = user,
          visibility = visibility,
          scms = scms,
          name = name,
          uri = uri,
          config = configData match {
            case None => Defaults.Config
            case Some(text) => Json.parse(text).as[Config]
          }
        )
      }
    }
  }


}

case class ProjectsWriteDao @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  db: Database,
  buildsDao: BuildsDao,
  buildsWriteDao: BuildsWriteDao,
  configsDao: ConfigsDao,
  membershipsDao: MembershipsDao,
  organizationsDao: OrganizationsDao,
  projectsDao: ProjectsDao,
  shasDao: ShasDao,
  shasWriteDao: ShasWriteDao,
  tagsDao: TagsDao,
  tagsWriteDao: TagsWriteDao
) {

  private[this] val InsertQuery = """
    insert into projects
    (id, user_id, organization_id, visibility, scms, name, uri, updated_by_user_id)
    values
    ({id}, {user_id}, {organization_id}, {visibility}, {scms}, {name}, {uri}, {updated_by_user_id})
  """

  private[this] val UpdateQuery = """
    update projects
       set visibility = {visibility},
           scms = {scms},
           name = {name},
           uri = {uri},
           updated_by_user_id = {updated_by_user_id}
     where id = {id}
  """

  private[this] val urlKey = UrlKey(minKeyLength = 2)

  def toSummary(project: Project): ProjectSummary = {
    ProjectSummary(
      id = project.id,
      organization = OrganizationSummary(project.organization.id),
      name = project.name,
      uri = project.uri
    )
  }

  private[db] def validate(
    user: UserReference,
    form: ProjectForm,
    existing: Option[Project] = None
  ): Seq[String] = {
    val uriErrors = if (form.uri.trim == "") {
      Seq("Uri cannot be empty")
    } else {
      form.scms match {
        case Scms.UNDEFINED(_) => Seq("Scms not found")
        case Scms.Github => {
          GithubUtil.parseUri(form.uri) match {
            case Left(error) => Seq(error)
            case Right(_) => Nil
          }
        }
      }
    }

    val visibilityErrors = form.visibility match {
      case Visibility.UNDEFINED(_) => Seq("Visibility must be one of: ${Visibility.all.map(_.toString).mkString(", ")}")
      case _ => Nil
    }

    val nameErrors = if (form.name.trim == "") {
      Seq("Name cannot be empty")

    } else {
      projectsDao.findByOrganizationIdAndName(Authorization.All, form.organization, form.name) match {
        case None => Seq.empty
        case Some(p) => {
          if (existing.map(_.id).contains(p.id)) {
            Nil
          } else {
            Seq("Project with this name already exists")
          }
        }
      }
    }

    val organizationErrors = membershipsDao.isMember(form.organization, user) match  {
      case false => Seq("You do not have access to this organization")
      case true => Nil
    }

    nameErrors ++ visibilityErrors ++ uriErrors ++ organizationErrors
  }

  def create(createdBy: UserReference, form: ProjectForm): Either[Seq[String], Project] = {
    validate(createdBy, form) match {
      case Nil => {

        val org = organizationsDao.findById(Authorization.All, form.organization).getOrElse {
          sys.error(s"Could not find organization with id[${form.organization}]")
        }
        
        val id = urlKey.generate(form.name.trim)

        db.withTransaction { implicit c =>
          SQL(InsertQuery).on(
            'id -> id,
            'organization_id -> org.id,
            'user_id -> createdBy.id,
            'visibility -> form.visibility.toString,
            'scms -> form.scms.toString,
            'name -> form.name.trim,
            'uri -> form.uri.trim,
            'updated_by_user_id -> createdBy.id
          ).execute()

          form.config.foreach { cfg =>
            configsDao.upsertWithConnection(c, createdBy, id, cfg)
          }

          form.config.getOrElse(Defaults.Config).builds.foreach { buildConfig =>
            buildsWriteDao.upsert(c, createdBy, id, Status.Enabled, buildConfig)
          }
        }

        mainActor ! MainActor.Messages.ProjectCreated(id)

        buildsDao.findAll(Authorization.All, projectId = Some(id), limit = None).foreach { build =>
          mainActor ! MainActor.Messages.BuildCreated(build.id)
        }

        Right(
          projectsDao.findById(Authorization.All, id).getOrElse {
            sys.error("Failed to create project")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def update(createdBy: UserReference, project: Project, form: ProjectForm): Either[Seq[String], Project] = {
    validate(createdBy, form, Some(project)) match {
      case Nil => {
        // To support org change - need to record the change as its
        // own record to be able to track changes.
        assert(
          project.organization.id == form.organization,
          "Changing organization ID not currently supported"
        )

        db.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'id -> project.id,
            'visibility -> form.visibility.toString,
            'scms -> form.scms.toString,
            'name -> form.name.trim,
            'uri -> form.uri.trim,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        mainActor ! MainActor.Messages.ProjectUpdated(project.id)

        Right(
          projectsDao.findById(Authorization.All, project.id).getOrElse {
            sys.error("Failed to create project")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def delete(deletedBy: UserReference, project: Project): Unit = {
    Pager.create { offset =>
      shasDao.findAll(Authorization.All, projectId = Some(project.id), offset = offset, limit = Some(1000))
    }.foreach { sha =>
      shasWriteDao.delete(deletedBy, sha)
    }

    Pager.create { offset =>
      tagsDao.findAll(Authorization.All, projectId = Some(project.id), offset = offset, limit = Some(1000))
    }.foreach { tag =>
      tagsWriteDao.delete(deletedBy, tag)
    }

    buildsDao.findAll(Authorization.All, projectId = Some(project.id), limit = None).foreach { build =>
      buildsWriteDao.delete(deletedBy, build)
    }

    configsDao.deleteByProjectId(deletedBy, project.id)

    db.withConnection { implicit c =>
      SQL("select delete_project({id})").on(
        'id -> project.id
      ).execute()
    }

    mainActor ! MainActor.Messages.ProjectDeleted(project.id)
  }


}
