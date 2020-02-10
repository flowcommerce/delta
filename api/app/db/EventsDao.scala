package db

import java.io.{PrintWriter, StringWriter}

import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.{Event, EventType}
import io.flow.postgresql.{OrderBy, Query}
import io.flow.util.IdGenerator
import play.api.db._

@javax.inject.Singleton
class EventsDao @javax.inject.Inject() (
  db: Database,
  delete: Delete
) {

  private[this] val BaseQuery = Query(s"""
    select events.id,
           events.created_at,
           events.type,
           events.summary,
           events.error,
           projects.id as project_id,
           projects.name as project_name,
           projects.uri as project_uri,
           projects.organization_id as project_organization_id
      from events
      join projects on events.project_id = projects.id
  """)

  private[this] val InsertQuery = """
    insert into events
    (id, project_id, type, summary, error, updated_by_user_id)
    values
    ({id}, {project_id}, {type}, {summary}, {error}, {updated_by_user_id})
  """

  /**
    * Create an event, returning its id
    */
  def create(createdBy: UserReference, projectId: String, `type`: EventType, summary: String, ex: Option[Throwable]): String = {
    `type` match {
      case EventType.UNDEFINED(_) => sys.error("Invalid type: " + `type`)
      case _ => {}
    }

    val error = ex.map { e =>
      val sw = new StringWriter
      e.printStackTrace(new PrintWriter(sw))
      sw.toString.trim
    }

    val id = IdGenerator("evt").randomId()

    db.withConnection { implicit c =>
      SQL(InsertQuery).on(
        Symbol("id") ->id,
        Symbol("project_id") ->projectId,
        Symbol("type") ->`type`.toString,
        Symbol("summary") ->summary.trim,
        Symbol("error") ->error,
        Symbol("updated_by_user_id") ->createdBy.id
      ).execute()
    }

    id
  }

  def delete(deletedBy: UserReference, event: Event): Unit = {
    delete.delete("events", deletedBy.id, event.id)
  }

  def findById(id: String): Option[Event] = {
    findAll(ids = Some(Seq(id)), limit = Some(1)).headOption
  }

  /**
   * @param numberMinutesSinceCreation If specified, we filter to events.created_at >= now() - interval 'x minutes'.
    *       This can be used to find recent events (say events created in past 5 minutes)
   */
  def findAll(
    ids: Option[Seq[String]] = None,
    projectId: Option[String] = None,
    `type`: Option[EventType] = None,
    summaryKeywords: Option[String] = None,
    numberMinutesSinceCreation: Option[Long] = None,
    hasError: Option[Boolean] = None,
    orderBy: OrderBy = OrderBy("-events.created_at, events.id"),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[Event] = {
    db.withConnection { implicit c =>
      BaseQuery.
        optionalIn(s"events.id", ids).
        equals(s"events.project_id", projectId).
        equals(s"events.type", `type`.map(_.toString)).
        and(summaryKeywords.map { _ =>
          "lower(events.summary) like '%' || lower({summary_keywords}) || '%'"
        }).bind("summary_keywords", summaryKeywords).
        and(numberMinutesSinceCreation.map { minutes =>
          s"events.created_at >= now() - interval '$minutes minutes'"
        }).
        and(
          hasError.map {
            case true => "events.error is not null"
            case false => "events.error is null"
          }
        ).
        orderBy(orderBy.sql).
        optionalLimit(limit).
        offset(offset).
        as(
          io.flow.delta.v0.anorm.parsers.Event.parser().*
        )
    }
  }

}
