package db

import javax.inject.{Inject, Singleton}

import akka.actor.ActorRef
import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.actors.MainActor
import io.flow.delta.api.lib.StateDiff
import io.flow.delta.lib.Semver
import io.flow.delta.v0.models.{Build, State, StateForm, Version}
import io.flow.postgresql.{Authorization, OrderBy, Query}
import play.api.db._
import play.api.libs.json._
import io.flow.delta.v0.models.json._

@Singleton
class BuildLastStatesDao @Inject()(
  buildsDao: BuildsDao,
  delete: Delete,
  @javax.inject.Named("main-actor") mainActor: ActorRef,
  db: Database
) {

  def onChange(mainActor: ActorRef, buildId: String) {
    mainActor ! MainActor.Messages.BuildDesiredStateUpdated(buildId)
  }

  private[this] val BaseQuery = Query(s"""
    select build_last_states.id,
           build_last_states.versions,
           build_last_states.timestamp,
           builds.id as build_id,
           builds.name as build_name,
           projects.id as build_project_id,
           projects.name as build_project_name,
           projects.uri as build_project_uri,
           projects.organization_id as build_project_organization_id
      from build_last_states
      join builds on builds.id = build_last_states.build_id
      join projects on projects.id = builds.project_id
  """)

  private[this] val LookupIdQuery = s"select id from build_last_states where build_id = {build_id} limit 1"

  private[this] val UpsertQuery = s"""
    insert into build_last_states
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
    update build_last_states
       set versions = {versions}::json,
           timestamp = now(),
           updated_by_user_id = {updated_by_user_id}
     where build_id = {build_id}
  """

  private[this] val idGenerator = io.flow.play.util.IdGenerator("bls")

  private[db] def validate(
    user: UserReference,
    build: Build,
    form: StateForm
  ): Seq[String] = {
    buildsDao.findById(Authorization.All, build.id) match {
      case None => Seq("Build not found")
      case Some(build) => Nil
    }
  }

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
    orderBy: OrderBy = OrderBy(s"-build_last_states.timestamp,-build_last_states.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[State] = {

    db.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "build_last_states",
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

  def create(createdBy: UserReference, build: Build, form: StateForm): Either[Seq[String], State] = {
    validate(createdBy, build, form) match {
      case Nil => {

        val id = idGenerator.randomId()

        db.withConnection { implicit c =>
          SQL(UpsertQuery).on(
            'id -> id,
            'build_id -> build.id,
            'versions -> Json.toJson(normalize(form.versions)).toString,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        onChange(mainActor, build.id)

        Right(
          findByBuildId(Authorization.All, build.id).getOrElse {
            sys.error(s"Failed to create last state for buildId[${build.id}]")
          }
        )
      }
      case errors => Left(errors)
    }
  }

  def upsert(createdBy: UserReference, build: Build, form: StateForm): Either[Seq[String], State] = {
    findByBuildId(Authorization.All, build.id) match {
      case None => create(createdBy, build, form)
      case Some(existing) => update(createdBy, build, existing, form)
    }
  }

  private[this] def update(createdBy: UserReference, build: Build, existing: State, form: StateForm): Either[Seq[String], State] = {

    validate(createdBy, build, form) match {
      case Nil => {
        db.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'build_id -> build.id,
            'versions -> Json.toJson(normalize(form.versions)).toString,
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        val updated = findByBuildId(Authorization.All, build.id).getOrElse {
          sys.error(s"Failed to update last state")
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
      delete.delete("build_last_states", deletedBy.id, id)
    }
  }

  private[this] def lookupId(buildId: String): Option[String] = {
    db.withConnection { implicit c =>
      SQL(LookupIdQuery).on(
        'build_id -> buildId
      ).as(SqlParser.get[String]("id").*).headOption
    }
  }

}
