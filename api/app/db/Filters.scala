package db

import io.flow.delta.v0.models.Visibility
import io.flow.postgresql.Authorization

trait Clause {

  def sql: String

  def and: String = s"and $sql"

}

object Clause {

  case object True extends Clause {
    override val sql: String = "true"
  }

  case object False extends Clause {
    override val sql: String = "false"
  }

  case class Or(conditions: Seq[String]) extends Clause {
    assert(!conditions.isEmpty, "Must have at least one condition")

    override val sql: String = conditions match {
      case Nil => "false"
      case one :: Nil => one
      case multi => "(" + multi.mkString(" or ") + ")"
    }

  }

  def single(condition: String): Clause = {
    assert(!condition.trim.isEmpty, "condition cannot be empty")
    Or(Seq(condition))
  }

}

case class Filters(auth: Authorization) {

  private[this] def publicVisibilityClause(column: String) = {
    s"$column = '${Visibility.Public}'"
  }

  def organizations(
    organizationIdColumn: String,
    visibilityColumnName: Option[String] = None
  ): Clause = {
    auth match {
      case Authorization.PublicOnly => {
        visibilityColumnName match {
          case None => Clause.False
          case Some(col) => Clause.single(publicVisibilityClause(col))
        }
      }

      case Authorization.All => {
        Clause.True
      }

      case Authorization.User(id) => {
        // TODO: Bind
        val userClause = s"$organizationIdColumn in (select organization_id from memberships where user_id = '$id')"
        visibilityColumnName match {
          case None => Clause.single(userClause)
          case Some(col) => Clause.Or(Seq(userClause, publicVisibilityClause(col)))
        }
      }

      case Authorization.Organization(id) => {
        val orgClause = s"$organizationIdColumn = '$id'"
        visibilityColumnName match {
          case None => Clause.single(orgClause)
          case Some(col) => Clause.Or(Seq(orgClause, publicVisibilityClause(col)))
        }
      }
      case _: Authorization.Partner => sys.error("Invalid Authorization.Partner")
      case _: Authorization.Session => sys.error("Invalid Authorization.Session")
    }
  }

  def users(userIdColumn: String): Clause = {
    auth match {
      case Authorization.PublicOnly => {
        Clause.False
      }
      case Authorization.All => {
        Clause.True
      }
      case Authorization.User(id) => {
        Clause.single(s"$userIdColumn = '$id'")
      }
      case Authorization.Organization(id) => {
        Clause.single(s"$userIdColumn in (select user_id from memberships where organization_id = '$id')")
      }
    }
  }

}
