package db

import anorm._
import play.api.db._
import play.api.Logger
import play.api.Play.current
import scala.util.{Failure, Success, Try}

@javax.inject.Singleton
class HealthchecksDao @javax.inject.Inject() () {

  private[this] val Query = "select 1 as num"

  def isHealthy(): Boolean = {
    Try {
      DB.withConnection { implicit c =>
        SQL(Query).as(SqlParser.long("num").*).headOption.getOrElse {
          sys.error(s"Query[$Query] returned no results")
        }
      }
    } match {
      case Success(_) => true
      case Failure(ex) => {
        Logger.error(s"DB healthcheck query failed", ex)
        false
      }
    }
  }
}
