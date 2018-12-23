package db

import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.models.{Variable, VariableForm}
import io.flow.play.util.IdGenerator
import io.flow.postgresql.{Authorization, OrderBy, Query}
import play.api.db._

@javax.inject.Singleton
class VariablesDao @javax.inject.Inject() (
  @NamedDatabase("default") db: Database
) {

  private[this] val idGenerator = IdGenerator("var")

  private[this] val BaseQuery = Query(s"""
    select variables.id,
           variables.organization_id,
           variables.key,
           variables.value
      from variables
      join organizations on organizations.id = variables.organization_id
  """)

  private[this] val UpsertQuery = """
    insert into variables
    (id, organization_id, key, value, updated_by_user_id)
    values
    ({id}, {organization_id}, {key}, {value}, {updated_by_user_id})
    on conflict (organization_id, key)
    do update
    set
      value = {value},
      updated_by_user_id = {updated_by_user_id}
  """

  private[this] def validate(form: VariableForm): Seq[String] = {
    val keyErrors = if (form.key.trim.isEmpty) {
      Seq("Key cannot be empty")
    } else {
      Nil
    }

    val valueErrors = if (form.value.trim.isEmpty) {
      Seq("Value cannot be empty")
    } else {
      Nil
    }

    keyErrors ++ valueErrors
  }

  def upsert(auth: Authorization, updatedBy: UserReference, form: VariableForm): Either[Seq[String], Variable] = {
    validate(form) match {
      case Nil => {
        db.withConnection { implicit c =>
          SQL(UpsertQuery).on(
            'id -> idGenerator.randomId,
            'organization_id -> form.organization,
            'key -> form.key,
            'value -> form.value,
            'updated_by_user_id -> updatedBy.id
          ).execute
        }

        Right(
          findByOrganizationAndKey(auth, form.organization, form.key).getOrElse {
            sys.error("Could not upsert variable org: ${form.organization}, key: ${form.key}")
          }
        )
      }

      case errors => Left(errors)
    }
  }

  def findById(auth: Authorization, id: String): Option[Variable] = {
    findAll(auth = auth, ids = Some(Seq(id)), limit = 1).headOption
  }

  def findByOrganizationAndKey(auth: Authorization, organization: String, key: String): Option[Variable] = {
    findAll(auth = auth, organization = Some(organization), key = Some(key), limit = 1).headOption
  }

  def findAll(
    auth: Authorization,
    organization: Option[String] = None,
    key: Option[String] = None,
    ids: Option[Seq[String]] = None,
    limit: Long = 25,
    offset: Long = 0,
    orderBy: OrderBy = OrderBy("-created_at", Some("variables"))
  ): Seq[Variable] = db.withConnection { implicit c =>
    Standards.query(
      BaseQuery,
      tableName = "variables",
      auth = Filters(auth).organizations("variables.organization_id"),
      ids = ids,
      orderBy = orderBy.sql,
      limit = limit,
      offset = offset
    ).
      equals("variables.key", key).
      as(io.flow.delta.v0.anorm.parsers.Variable.parser().*)
  }

}
