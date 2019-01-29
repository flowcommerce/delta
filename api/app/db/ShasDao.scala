package db

import anorm._
import io.flow.common.v0.models.UserReference
import io.flow.delta.actors.MainActor
import io.flow.delta.v0.models.Sha
import io.flow.postgresql.{Authorization, OrderBy, Query}
import io.flow.util.IdGenerator
import play.api.db._

case class ShaForm(
  projectId: String,
  branch: String,
  hash: String
)

@javax.inject.Singleton
class ShasDao @javax.inject.Inject() (
  db: Database
) {

  private[this] val BaseQuery = Query(s"""
    select shas.id,
           shas.created_at,
           shas.branch,
           shas.hash,
           projects.id as project_id,
           projects.name as project_name,
           projects.uri as project_uri,
           projects.organization_id as project_organization_id
      from shas
      join projects on shas.project_id = projects.id
  """)


  def findByProjectIdAndBranch(auth: Authorization, projectId: String, branch: String): Option[Sha] = {
    findAll(auth, projectId = Some(projectId), branch = Some(branch), limit = Some(1)).headOption
  }

  def findById(auth: Authorization, id: String): Option[Sha] = {
    findAll(auth, ids = Some(Seq(id)), limit = Some(1)).headOption
  }

  def findAll(
    auth: Authorization,
    ids: Option[Seq[String]] = None,
    projectId: Option[String] = None,
    branch: Option[String] = None,
    hash: Option[String] = None,
    orderBy: OrderBy = OrderBy("lower(shas.branch), shas.created_at"),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[Sha] = {

    db.withConnection { implicit c =>
      Standards.query(
        BaseQuery,
        tableName = "shas",
        auth = Filters(auth).organizations("projects.organization_id"),
        ids = ids,
        orderBy = orderBy.sql,
        limit = limit,
        offset = offset
      ).
        equals("projects.id", projectId).
        optionalText(
          "shas.branch",
          branch,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        optionalText(
          "shas.hash",
          hash,
          columnFunctions = Seq(Query.Function.Lower),
          valueFunctions = Seq(Query.Function.Lower, Query.Function.Trim)
        ).
        as(
          io.flow.delta.v0.anorm.parsers.Sha.parser().*
        )
    }
  }

}

case class ShasWriteDao @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef,
  db: Database,
  projectsDao: ProjectsDao,
  shasDao: ShasDao,
  delete: Delete
) {

  private[this] val UpsertQuery = """
    insert into shas
    (id, project_id, branch, hash, updated_by_user_id)
    values
    ({id}, {project_id}, {branch}, {hash}, {updated_by_user_id})
    on conflict (project_id, branch)
    do update
       set hash = {hash},
           updated_by_user_id = {updated_by_user_id}
  """

  private[db] def validate(
    form: ShaForm,
    existing: Option[Sha] = None
  ): Seq[String] = {
    val hashErrors = if (form.hash.trim == "") {
      Seq("Hash cannot be empty")
    } else {
      Nil
    }

    val branchErrors = if (form.branch.trim == "") {
      Seq("Branch cannot be empty")
    } else {
      Nil
    }

    val projectErrors = projectsDao.findById(Authorization.All, form.projectId) match {
      case None => Seq("Project not found")
      case Some(_) => Nil
    }

    val existingErrors = shasDao.findByProjectIdAndBranch(Authorization.All, form.projectId, form.branch) match {
      case None => Nil
      case Some(found) => {
        existing.map(_.id) == Some(found.id) match {
          case true => Nil
          case false => Seq("Project already has a hash for this branch")
        }
      }
    }

    hashErrors ++ branchErrors ++ projectErrors ++ existingErrors
  }

  def create(createdBy: UserReference, form: ShaForm): Either[Seq[String], Sha] = {
    validate(form) match {
      case Nil => Right(upsert(createdBy, form))
      case errors => Left(errors)
    }
  }

  /**
    * Sets the value of the hash for the master branch, creating or
    * updated the sha record as needed. Returns the created or updated
    * sha.
    */
  def upsertBranch(createdBy: UserReference, projectId: String, branch: String, hash: String): Sha = {
    val form = ShaForm(
      projectId = projectId,
      branch = branch,
      hash = hash
    )
    upsert(createdBy, form)
  }

  private[this] def upsert(createdBy: UserReference, form: ShaForm): Sha = {
    val newId = IdGenerator("sha").randomId()

    db.withConnection { implicit c =>
      SQL(UpsertQuery).on(
        'id -> newId,
        'project_id -> form.projectId.trim,
        'branch -> form.branch.trim,
        'hash -> form.hash.trim,
        'updated_by_user_id -> createdBy.id
      ).execute()
    }

    val sha = shasDao.findByProjectIdAndBranch(Authorization.All, form.projectId, form.branch).getOrElse {
      sys.error(s"Failed to upsert projectId[${form.projectId}] branch[${form.branch}]")
    }

    mainActor ! MainActor.Messages.ShaUpserted(form.projectId, sha.id)

    sha
  }

  def delete(deletedBy: UserReference, sha: Sha): Unit = {
    delete.delete("shas", deletedBy.id, sha.id)
  }

}
