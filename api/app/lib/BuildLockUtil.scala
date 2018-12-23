package io.flow.delta.api.lib

import anorm.SqlParser
import io.flow.postgresql.Query
import play.api.db._

@javax.inject.Singleton
class BuildLockUtil @javax.inject.Inject() (
  db: Database
) {
  private[this] val tableName: String = "builds"

  private[this] val LockQuery = Query(s"""
    select id
      from $tableName
     where id = {id}
       for update
  """)

  /**
   *  Lock on builds.id during execution of provided function.
   */
  def withLock(id: String)(
    f: => Unit
  ): Unit = {
    db.withTransaction { implicit c =>
      LockQuery.bind("id", id).as(SqlParser.str("id").singleOpt).map(_ => f).getOrElse {
        sys.error(s"Attempted to lock for id [$id], but could not find a corresponding entry in $tableName.")
      }
    }
  }
}