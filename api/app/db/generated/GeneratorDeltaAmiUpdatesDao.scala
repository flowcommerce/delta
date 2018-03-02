package db.generated

import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.postgresql.{OrderBy, Query}
import io.flow.postgresql.play.db.DbHelpers
import java.sql.Connection
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.{Database, NamedDatabase}

case class AmiUpdate(
  id: String,
  name: String,
  createdAt: DateTime
) {

  lazy val form: AmiUpdateForm = AmiUpdateForm(
    id = id,
    name = name
  )

}

case class AmiUpdateForm(
  id: String,
  name: String
)

@Singleton
class AmiUpdatesDao @Inject() (
  @NamedDatabase("default") db: Database
) {

  private[this] val dbHelpers = DbHelpers(db, "ami_updates")

  private[this] val BaseQuery = Query("""
      | select ami_updates.id,
      |        ami_updates.name,
      |        ami_updates.created_at,
      |        ami_updates.hash_code
      |   from ami_updates
  """.stripMargin)

  private[this] val InsertQuery = Query("""
    | insert into ami_updates
    | (id, name, hash_code)
    | values
    | ({id}, {name}, {hash_code}::bigint)
  """.stripMargin)

  private[this] val UpdateQuery = Query("""
    | update ami_updates
    |    set name = {name},
    |        hash_code = {hash_code}::bigint
    |  where id = {id}
    |    and ami_updates.hash_code != {hash_code}::bigint
  """.stripMargin)

  private[this] def bindQuery(query: Query, form: AmiUpdateForm): Query = {
    query.
      bind("name", form.name).
      bind("hash_code", form.hashCode())
  }

  def insert(updatedBy: UserReference, form: AmiUpdateForm) {
    db.withConnection { implicit c =>
      insert(c, updatedBy, form)
    }
  }

  def insert(implicit c: Connection, updatedBy: UserReference, form: AmiUpdateForm) {
    bindQuery(InsertQuery, form).
      bind("id", form.id).
      bind("updated_by_user_id", updatedBy.id).
      anormSql.execute()
  }

  def updateIfChangedById(updatedBy: UserReference, id: String, form: AmiUpdateForm) {
    if (!findById(id).map(_.form).contains(form)) {
      updateById(updatedBy, id, form)
    }
  }

  def updateById(updatedBy: UserReference, id: String, form: AmiUpdateForm) {
    db.withConnection { implicit c =>
      updateById(c, updatedBy, id, form)
    }
  }

  def updateById(implicit c: Connection, updatedBy: UserReference, id: String, form: AmiUpdateForm) {
    bindQuery(UpdateQuery, form).
      bind("id", id).
      bind("updated_by_user_id", updatedBy.id).
      anormSql.execute()
  }

  def update(updatedBy: UserReference, existing: AmiUpdate, form: AmiUpdateForm) {
    db.withConnection { implicit c =>
      update(c, updatedBy, existing, form)
    }
  }

  def update(implicit c: Connection, updatedBy: UserReference, existing: AmiUpdate, form: AmiUpdateForm) {
    updateById(c, updatedBy, existing.id, form)
  }

  def delete(deletedBy: UserReference, amiUpdate: AmiUpdate) {
    dbHelpers.delete(deletedBy, amiUpdate.id)
  }

  def deleteById(deletedBy: UserReference, id: String) {
    db.withConnection { implicit c =>
      deleteById(c, deletedBy, id)
    }
  }

  def deleteById(c: java.sql.Connection, deletedBy: UserReference, id: String) {
    dbHelpers.delete(c, deletedBy, id)
  }

  def findById(id: String): Option[AmiUpdate] = {
    db.withConnection { implicit c =>
      findByIdWithConnection(c, id)
    }
  }

  def findByIdWithConnection(c: java.sql.Connection, id: String): Option[AmiUpdate] = {
    findAllWithConnection(c, ids = Some(Seq(id)), limit = 1).headOption
  }

  def findAll(
    ids: Option[Seq[String]] = None,
    limit: Long,
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("ami_updates.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[AmiUpdate] = {
    db.withConnection { implicit c =>
      findAllWithConnection(
        c,
        ids = ids,
        limit = limit,
        offset = offset,
        orderBy = orderBy
      )(customQueryModifier)
    }
  }

  def findAllWithConnection(
    c: java.sql.Connection,
    ids: Option[Seq[String]] = None,
    limit: Long,
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("ami_updates.id")
  ) (
    implicit customQueryModifier: Query => Query = { q => q }
  ): Seq[AmiUpdate] = {
    customQueryModifier(BaseQuery).
      optionalIn("ami_updates.id", ids).
      limit(limit).
      offset(offset).
      orderBy(orderBy.sql).
      as(AmiUpdatesDao.parser().*)(c)
  }

}

object AmiUpdatesDao {

  def parser(): RowParser[AmiUpdate] = {
    SqlParser.str("id") ~
    SqlParser.str("name") ~
    SqlParser.get[DateTime]("created_at") map {
      case id ~ name ~ createdAt => AmiUpdate(
        id = id,
        name = name,
        createdAt = createdAt
      )
    }
  }

}