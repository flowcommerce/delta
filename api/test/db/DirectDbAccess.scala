package db

import anorm._
import io.flow.test.utils.FlowPlaySpec

object DirectDbAccess extends FlowPlaySpec with db.Helpers {

  def setCreatedAt(table: String, id: String, minutes: Int) {
    database.withConnection { implicit c =>
      SQL(s"update $table set created_at = now() + interval '$minutes minutes' where id = {id}").on(
        'id -> id
      ).executeUpdate()
    }
  }

}
