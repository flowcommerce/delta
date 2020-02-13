package db.generated

import anorm._
import anorm.JodaParameterMetaData._
import io.flow.common.v0.models.UserReference
import io.flow.postgresql.{OrderBy, Query}
import io.flow.postgresql.play.db.DbHelpers
import io.flow.util.IdGenerator
import java.sql.Connection
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.Database
import play.api.libs.json.{JsObject, JsValue, Json}

case class BuildLastStates(
  id: String,
  buildId: String,
  timestamp: DateTime,
  versions: Seq[JsObject]
) {

  lazy val form: BuildLastStatesForm = BuildLastStatesForm(
    buildId = buildId,
    timestamp = timestamp,
    versions = versions
  )

}

case class BuildLastStatesForm(
  buildId: String,
  timestamp: DateTime,
  versions: Seq[JsValue]
) {
  assert(
    versions.forall(_.isInstanceOf[JsObject]),
    s"Field[versions] must contain JsObjects and not a ${versions.filterNot(_.isInstanceOf[JsObject]).map(_.getClass.getName).distinct}"
  )
}

@Singleton
class BuildLastStatesDao @Inject() (
  db: Database
) {

  private[this] val idGenerator = IdGenerator("bls")

  def randomId(): String = idGenerator.randomId()

  private[this] val dbHelpers = DbHelpers(db, "build_last_states")

  private[this] val BaseQuery = Query("""
      | select build_last_states.id,
      |        build_last_states.build_id,
      |        build_last_states.timestamp,
      |        build_last_states.versions::text as versions_text,
      |        build_last_states.created_at,
      |        build_last_states.updated_at,
      |        build_last_states.updated_by_user_id,
      |        build_last_states.hash_code
      |   from build_last_states
  """.stripMargin)

  private[this] val UpsertQuery = Query("""
    | insert into build_last_states
    | (id, build_id, timestamp, versions, updated_by_user_id, hash_code)
    | values
    | ({id}, {build_id}, {timestamp}::timestamptz, {versions}::json, {updated_by_user_id}, {hash_code}::bigint)
    | on conflict (build_id)
    | do update
    |    set timestamp = {timestamp}::timestamptz,
    |        versions = {versions}::json,
    |        updated_by_user_id = {updated_by_user_id},
    |        hash_code = {hash_code}::bigint
    |  where build_last_states.hash_code != {hash_code}::bigint
    | returning id
  """.stripMargin)

  private[this] val UpdateQuery = Query("""
    | update build_last_states
    |    set build_id = {build_id},
    |        timestamp = {timestamp}::timestamptz,
    |        versions = {versions}::json,
    |        updated_by_user_id = {updated_by_user_id},
    |        hash_code = {hash_code}::bigint
    |  where id = {id}
    |    and build_last_states.hash_code != {hash_code}::bigint
  """.stripMargin)

  private[this] def bindQuery(query: Query, form: BuildLastStatesForm): Query = {
    query.
      bind("build_id", form.buildId).
      bind("timestamp", form.timestamp).
      bind("versions", Json.toJson(form.versions)).
      bind("hash_code", form.hashCode())
  }

  private[this] def toNamedParameter(updatedBy: UserReference, form: BuildLastStatesForm): Seq[NamedParameter] = {
    Seq(
      Symbol("id") ->randomId(),
      Symbol("build_id") ->form.buildId,
      Symbol("timestamp") ->form.timestamp,
      Symbol("versions") ->Json.toJson(form.versions).toString,
      Symbol("updated_by_user_id") ->updatedBy.id,
      Symbol("hash_code") ->form.hashCode()
    )
  }

  def upsertIfChangedByBuildId(updatedBy: UserReference, form: BuildLastStatesForm): Unit = {
    if (!findByBuildId(form.buildId).map(_.form).contains(form)) {
      upsertByBuildId(updatedBy, form)
    }
  }

  def upsertByBuildId(updatedBy: UserReference, form: BuildLastStatesForm): Unit = {
    db.withConnection { implicit c =>
      upsertByBuildId(c, updatedBy, form)
    }
  }

  def upsertByBuildId(implicit c: Connection, updatedBy: UserReference, form: BuildLastStatesForm): Unit = {
    bindQuery(UpsertQuery, form).
      bind("id", randomId()).
      bind("updated_by_user_id", updatedBy.id).
      anormSql.execute()
  }

  def upsertBatchByBuildId(updatedBy: UserReference, forms: Seq[BuildLastStatesForm]): Unit = {
    db.withConnection { implicit c =>
      upsertBatchByBuildId(c, updatedBy, forms)
    }
  }

  def upsertBatchByBuildId(implicit c: Connection, updatedBy: UserReference, forms: Seq[BuildLastStatesForm]): Unit = {
    if (forms.nonEmpty) {
      val params = forms.map(toNamedParameter(updatedBy, _))
      BatchSql(UpsertQuery.sql(), params.head, params.tail: _*).execute()
    }
  }

  def updateIfChangedById(updatedBy: UserReference, id: String, form: BuildLastStatesForm): Unit = {
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }

  def updateById(updatedBy: UserReference, id: String, form: BuildLastStatesForm): Unit = {
    db.withConnection { implicit c =>
      updateById(c, updatedBy, id, form)
    }
  }

  def updateById(implicit c: Connection, updatedBy: UserReference, id: String, form: BuildLastStatesForm): Unit = {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      bind("updated_by_user_id", updatedBy.id).
      anormSql.execute()
  }

  def update(updatedBy: UserReference, existing: BuildLastStates, form: BuildLastStatesForm): Unit = {
    db.withConnection { implicit c =>
      update(c, updatedBy, existing, form)
    }
  }

  def update(implicit c: Connection, updatedBy: UserReference, existing: BuildLastStates, form: BuildLastStatesForm): Unit = {
    updateById(c, updatedBy, existing.id, form)
  }

  def delete(deletedBy: UserReference, buildLastStates: BuildLastStates): Unit = {
    dbHelpers.delete(deletedBy, buildLastStates.id)
  }

  def deleteById(deletedBy: UserReference, id: String): Unit = {
    db.withConnection { implicit c =>
      deleteById(c, deletedBy, id)
    }
  }

  def deleteById(c: java.sql.Connection, deletedBy: UserReference, id: String): Unit = {
    dbHelpers.delete(c, deletedBy, id)
  }

  def deleteByBuildId(deletedBy: UserReference, buildId: String): Unit = {
    findByBuildId(buildId).foreach { r =>
      delete(deletedBy, r)
    }
  }

  def findById(id: String): Option[BuildLastStates] = {
    db.withConnection { implicit c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(c: java.sql.Connection, id: String): Option[BuildLastStates] = {
    findAllWithConnection(c, ids = Some(Seq(id)), limit = Some(1)).headOption
  }

  def findByBuildId(buildId: String): Option[BuildLastStates] = {
    db.withConnection { implicit c =>
      findByBuildIdWithConnection(c, buildId)
    }
  }

  def findByBuildIdWithConnection(c: java.sql.Connection, buildId: String): Option[BuildLastStates] = {
    findAllWithConnection(c, buildId = Some(buildId), limit = Some(1)).headOption
  }

  def findAll(
    ids: Option[Seq[String]] = None,
    buildId: Option[String] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("build_last_states.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[BuildLastStates] = {
    db.withConnection { implicit c =>
      findAllWithConnection(
        c,
        ids = ids,
        buildId = buildId,
        limit = limit,
        offset = offset,
        orderBy = orderBy
      )(customQueryModifier)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    ids: Option[Seq[String]] = None,
    buildId: Option[String] = None,
    limit: Option[Long],
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("build_last_states.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[BuildLastStates] = {
    customQueryModifier(BaseQuery).
      optionalIn("build_last_states.id", ids).
      equals("build_last_states.build_id", buildId).
      optionalLimit(limit).
      offset(offset).
      orderBy(orderBy.sql).
      as(BuildLastStatesDao.parser.*)(c)
  }

}

object BuildLastStatesDao {

  val parser: RowParser[BuildLastStates] = {
    SqlParser.str("id") ~
    SqlParser.str("build_id") ~
    SqlParser.get[DateTime]("timestamp") ~
    SqlParser.str("versions_text") map {
      case id ~ buildId ~ timestamp ~ versions => BuildLastStates(
        id = id,
        buildId = buildId,
        timestamp = timestamp,
        versions = Json.parse(versions).as[Seq[JsObject]]
      )
    }
  }

}