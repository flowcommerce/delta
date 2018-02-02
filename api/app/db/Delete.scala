package db

import anorm._
import play.api.db._

@javax.inject.Singleton
class Delete @javax.inject.Inject() (
  @NamedDatabase("default") db: Database
) {

  private[this] val Query = """
    select util.delete_by_id({user_id}, {table}, {id})
  """

  def delete(table: String, deletedById: String, id: String) {
    db.withConnection { implicit c =>
      delete(c, table, deletedById, id)
    }
  }

  def delete(
    implicit c: java.sql.Connection,
    table: String, deletedById: String, id: String
  ) {
    SQL(Query).on(
      'id -> id,
      'table -> table,
      'user_id -> deletedById
    ).execute()
  }

}
