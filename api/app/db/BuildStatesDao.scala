package db

import akka.actor.ActorRef
import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.actors.MainActor
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.lib.Semver
import io.flow.delta.v0.models.{Build, Project, State, StateForm, Version}
import io.flow.delta.v0.models.json._
import io.flow.postgresql.{Authorization, Query, OrderBy}
import play.api.db._
import play.api.libs.json._
import play.api.Play.current

object BuildDesiredStatesDao extends BuildStatesDao("build_desired_states")

case class BuildDesiredStatesWriteDao @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: ActorRef
) extends BuildStatesWriteDao(BuildDesiredStatesDao, mainActor, "bds") {

  override def onChange(mainActor: ActorRef, buildId: String) {
    mainActor ! MainActor.Messages.BuildDesiredStateUpdated(buildId)
  }

}


object BuildLastStatesDao extends BuildStatesDao("build_last_states")

case class BuildLastStatesWriteDao @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: ActorRef
) extends BuildStatesWriteDao(BuildLastStatesDao, mainActor, "bls") {

  override def onChange(mainActor: ActorRef, buildId: String) {
    mainActor ! MainActor.Messages.BuildLastStateUpdated(buildId)
  }

}

private[db] class BuildStatesDao(val table: String) {
  
  private[this] val BaseQuery = Query(s"""
    select ${table}.id,
           ${table}.versions,
           ${table}.timestamp,
           builds.id as build_id,
           builds.name as build_name,
           projects.id as build_project_id,
           projects.name as build_project_name,
           projects.uri as build_project_uri,
           projects.organization_id as build_project_organization_id
      from $table
      join builds on builds.id = ${table}.build_id
      join projects on projects.id = builds.project_id
  """)


  def findByBuildId(auth: Authorization, buildId: String): Option[State] = {
    findAll(auth, buildId = Some(buildId), limit = 1).headOption
  }

  def findById(auth: Authorization, id: String): Option[State] = {
    findAll(auth, ids = Some(Seq(id)), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    ids: Option[Seq[String]] = None,
    buildId: Option[String] = None,
    orderBy: OrderBy = OrderBy(s"-${table}.timestamp,-${table}.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[State] = {

    DB.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = table,
        auth = Filters(auth).organizations("projects.organization_id"),
        ids = ids,
        orderBy = orderBy.sql,
        limit = limit,
        offset = offset
      ).
        equals("builds.id", buildId).
        as(
          io.flow.delta.v0.anorm.parsers.State.parser().*
        )
    }
  }
  
}

private[db] class BuildStatesWriteDao(
  reader: BuildStatesDao,
  mainActor: ActorRef,
  idPrefix: String
) {

  private[this] val table = reader.table

  /**
    * Invoked whenever a state record is created or updated (when
    * something in the versions actually changed)
    */
  def onChange(mainActor: ActorRef, buildId: String) {
    // No-op
  }
  
  private[this] val LookupIdQuery = s"select id from $table where build_id = {build_id} limit 1"

  private[this] val UpsertQuery = s"""
    insert into $table
    (id, build_id, versions, timestamp, updated_by_user_id)
    values
    ({id}, {build_id}, {versions}::json, now(), {updated_by_user_id})
    on conflict(build_id)
    do update
          set versions = {versions}::json,
          timestamp = now(),
          updated_by_user_id = {updated_by_user_id}
  """

  private[this] val UpdateQuery = s"""
    update $table
       set versions = {versions}::json,
           timestamp = now(),
           updated_by_user_id = {updated_by_user_id}
     where build_id = {build_id}
  """

  private[this] val idGenerator = io.flow.play.util.IdGenerator(idPrefix)

  private[db] def validate(
    user: UserReference,
    build: Build,
    form: StateForm
  ): Seq[String] = {
    BuildsDao.findById(Authorization.All, build.id) match {
      case None => Seq("Build not found")
      case Some(build) => Nil
    }
  }

  def create(createdBy: UserReference, build: Build, form: StateForm): Either[Seq[String], State] = {
    validate(createdBy, build, form) match {
      case Nil => {

        val id = idGenerator.randomId()

        DB.withConnection { implicit c =>
          SQL(UpsertQuery).on(
            'id -> id,
            'build_id -> build.id,
            'versions -> Json.toJson(normalize(form.versions)).toString,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        onChange(mainActor, build.id)

        Right(
          reader.findByBuildId(Authorization.All, build.id).getOrElse {
            sys.error(s"Failed to create $table for buildId[${build.id}]")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def upsert(createdBy: UserReference, build: Build, form: StateForm): Either[Seq[String], State] = {
    reader.findByBuildId(Authorization.All, build.id) match {
      case None => create(createdBy, build, form)
      case Some(existing) => update(createdBy, build, existing, form)
    }
  }

  private[this] def update(createdBy: UserReference, build: Build, existing: State, form: StateForm): Either[Seq[String], State] = {

    validate(createdBy, build, form) match {
      case Nil => {
        DB.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'build_id -> build.id,
            'versions -> Json.toJson(normalize(form.versions)).toString,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        val updated = reader.findByBuildId(Authorization.All, build.id).getOrElse {
          sys.error(s"Failed to update $table")
        }

        StateDiff.diff(existing.versions, updated.versions) match {
          case Nil => // No-op
          case _ => onChange(mainActor, build.id)
        }

        Right(updated)
      }
      case errors => Left(errors)
    }
  }

  /**
    * Only include versions w at least 1 instance
    * Sort deterministically
    */
  private[this] def normalize(versions: Seq[Version]): Seq[Version] = {
    versions.
      filter { v => v.instances > 0 }.
      sortBy { v =>
        Semver.parse(v.name) match {
          case None => s"9:$v"
          case Some(tag) => s"1:${tag.sortKey}"
        }
      }
  }
  
  def delete(deletedBy: UserReference, build: Build) {
    lookupId(build.id).map { id =>
      Delete.delete(table, deletedBy.id, id)
    }
  }

  private[this] def lookupId(buildId: String): Option[String] = {
    DB.withConnection { implicit c =>
      SQL(LookupIdQuery).on(
        'build_id -> buildId
      ).as(SqlParser.get[String]("id").*).headOption
    }
  }

}
