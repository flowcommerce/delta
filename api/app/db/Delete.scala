package db

import anorm._
import play.api.db._
import play.api.Play.current

object Delete {

  private[this] val Query = """
    select util.delete_by_id({user_id}, {table}, {id})
  """

  def delete(table: String, deletedById: String, id: String) {
    DB.withConnection { implicit c =>
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
