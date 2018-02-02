package io.flow.delta.api.lib

import javax.inject.{Inject, Singleton}

import anorm.{SQL, SqlParser}
import io.flow.postgresql.Query
import play.api.db._
import play.api.Play.current

case class BuildLockUtil () {
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
    DB.withTransaction { implicit c =>
      LockQuery.bind("id", id).as(SqlParser.str("id").singleOpt).map(_ => f).getOrElse {
        sys.error(s"Attempted to lock for id [$id], but could not find a corresponding entry in $tableName.")
      }
    }
  }
}