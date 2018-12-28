package db

import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.config.v0.models.Config
import io.flow.delta.config.v0.models.json._
import io.flow.delta.v0.models.Reference
import io.flow.postgresql.{Authorization, OrderBy, Query}
import io.flow.util.IdGenerator

import play.api.db._
import play.api.libs.json._

case class InternalConfig(
  id: String,
  project: Reference,
  config: Config
)

@javax.inject.Singleton
class ConfigsDao @javax.inject.Inject() (
  db: Database,
  delete: Delete
) {

  private[this] val BaseQuery = Query(s"""
    select configs.id,
           configs.project_id,
           configs.data::varchar
      from configs
      join projects on projects.id = configs.project_id
  """)

  private[this] val UpsertQuery = """
    insert into configs
    (id, project_id, data, updated_by_user_id)
    values
    ({id}, {project_id}, {data}::json, {updated_by_user_id})
    on conflict(project_id)
    do update
          set data = {data}::json,
              updated_by_user_id = {updated_by_user_id}
  """

  private[this] lazy val idGenerator = IdGenerator("cfg")

  def findByProjectId(auth: Authorization, projectId: String): Option[InternalConfig] = {
    findAll(auth, projectId = Some(projectId), limit = 1).headOption
  }

  def findById(auth: Authorization, id: String): Option[InternalConfig] = {
    findAll(auth, ids = Some(Seq(id)), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    ids: Option[Seq[String]] = None,
    projectId: Option[String] = None,
    orderBy: OrderBy = OrderBy("-configs.created_at"),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[InternalConfig] = {

    db.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "configs",
        auth = Filters(auth).organizations("projects.organization_id"),
        ids = ids,
        orderBy = orderBy.sql,
        limit = limit,
        offset = offset
      ).
        equals("configs.project_id", projectId).
        as(
          parser().*
        )
    }
  }

  private[this] def parser(): RowParser[InternalConfig] = {
    SqlParser.str("id") ~
    SqlParser.str("project_id") ~
    SqlParser.str("data") map {
      case id ~ projectId ~ data => {
        InternalConfig(
          id = id,
          project = Reference(projectId),
          config = Json.parse(data).as[Config]
        )
      }
    }
  }

  def updateIfChanged(createdBy: UserReference, projectId: String, newConfig: Config): Unit = {
    val existing: Option[Config] = findByProjectId(Authorization.All, projectId).map(_.config)

    existing match {
      case None => {
        upsert(createdBy, projectId, newConfig)
        ()
      }

      case Some(ex) => {
        ex == newConfig match {
          case false => {
            upsert(createdBy, projectId, newConfig)
            ()
          }
          case true => {
            // noop - no change
          }
        }
      }
    }
  }

  def upsert(createdBy: UserReference, projectId: String, config: Config): InternalConfig = {
    db.withConnection { implicit c =>
      upsertWithConnection(c, createdBy, projectId, config)
    }

    findByProjectId(Authorization.All, projectId).getOrElse {
      sys.error(s"Failed to create configuration for projectId[$projectId]")
    }
  }

  private[db] def upsertWithConnection(implicit c: java.sql.Connection, createdBy: UserReference, projectId: String, config: Config): Unit = {
    SQL(UpsertQuery).on(
      'id -> idGenerator.randomId(),
      'project_id -> projectId,
      'data -> Json.toJson(config).toString,
      'updated_by_user_id -> createdBy.id
    ).execute()
    ()
  }

  def deleteByProjectId(deletedBy: UserReference, projectId: String): Unit = {
    findByProjectId(Authorization.All, projectId).foreach { internal =>
      delete(deletedBy, internal)
    }
  }

  def delete(deletedBy: UserReference, internal: InternalConfig): Unit = {
    delete.delete("configs", deletedBy.id, internal.id)
  }

}
