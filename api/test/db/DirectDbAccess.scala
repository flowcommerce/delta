package db

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

object DirectDbAccess {

  def setCreatedAt(table: String, id: String, minutes: Int) {
    DB.withConnection { implicit c =>
      SQL(s"update $table set created_at = now() + interval '$minutes minutes' where id = {id}").on(
        'id -> id
      ).executeUpdate()
    }
  }

}
