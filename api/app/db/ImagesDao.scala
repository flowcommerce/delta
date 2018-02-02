package db

import io.flow.delta.actors.MainActor
import io.flow.delta.lib.Semver
import io.flow.delta.v0.models._
import io.flow.postgresql.{Authorization, Query, OrderBy}
import io.flow.common.v0.models.UserReference
import anorm._
import play.api.db._
import play.api.Play.current

object ImagesDao {

  private[this] val BaseQuery = Query(s"""
    select images.id,
           images.build_id,
           images.name,
           images.version,
           builds.id as build_id,
           builds.name as build_name,
           builds.status as build_status,
           projects.id as build_project_id,
           projects.name as build_project_name,
           projects.uri as build_project_uri,
           projects.organization_id as build_project_organization_id
      from images
      join builds on builds.id = images.build_id
      join projects on projects.id = builds.project_id
  """)

  def findById(id: String): Option[Image] = {
    findAll(ids = Some(Seq(id)), limit = 1).headOption
  }

  def findByBuildIdAndVersion(buildId: String, version: String): Option[Image] = {
    findAll(
      buildId = Some(buildId),
      versions = Some(Seq(version)),
      limit = 1
    ).headOption
  }

  def findAll(
   ids: Option[Seq[String]] = None,
   buildId: Option[String] = None,
   names: Option[Seq[String]] = None,
   versions: Option[Seq[String]] = None,
   orderBy: OrderBy = OrderBy("lower(images.name),-images.sort_key"),
   limit: Long = 25,
   offset: Long = 0
  ): Seq[Image] = {
    DB.withConnection { implicit c =>
      BaseQuery.
        optionalIn("images.id", ids).
        optionalIn("images.name", names).
        equals("images.build_id", buildId).
        optionalIn("images.version", versions).
        orderBy(orderBy.sql).
        limit(limit).
        offset(offset).
        as(
          io.flow.delta.v0.anorm.parsers.Image.parser().*
        )
    }
  }
  
}

case class ImagesWriteDao @javax.inject.Inject() (
  @javax.inject.Named("main-actor") mainActor: akka.actor.ActorRef
) {

  private[this] val UpsertQuery = """
    insert into images
    (id, build_id, name, version, sort_key, updated_by_user_id)
    values
    ({id}, {build_id}, {name}, {version}, {sort_key}, {updated_by_user_id})
    on conflict(build_id, version)
    do update
          set name = {name},
              sort_key = {sort_key},
              updated_by_user_id = {updated_by_user_id}
  """

  private[this] val UpdateQuery = """
    update images
       set build_id = {build_id},
           name = {name},
           version = {version},
           sort_key = {sort_key},
           updated_by_user_id = {updated_by_user_id}
     where id = {id}
  """

  private[db] def validate(
    user: UserReference,
    form: ImageForm,
    existing: Option[Image] = None
    ): Seq[String] = {
    val nameErrors = if (form.name.trim == "") {
      Seq("Name cannot be empty")
    } else {
      Nil
    }

    val versionErrors = if (form.version.trim == "") {
      Seq("Version cannot be empty")
    } else {
      Semver.isSemver(form.version.trim) match {
        case true => Nil
        case false => Seq("Version must match semver pattern (e.g. 0.1.2)")
      }
    }

    val buildErrors = BuildsDao.findById(Authorization.All, form.buildId) match {
      case None => Seq("Build not found")
      case Some(build) => Nil
    }

    nameErrors ++ versionErrors ++ buildErrors
  }

  /**
    * If the tag exists, updates the hash to match (if
    * necessary). Otherwise creates the tag.
    */
  def upsert(createdBy: UserReference, buildId: String, name: String, version: String): Image = {
    val form = ImageForm(
      buildId = buildId,
      name = name,
      version = version
    )
    ImagesDao.findByBuildIdAndVersion(buildId, version) match {
      case None => {
        create(createdBy, form) match {
          case Left(errors) => sys.error(errors.mkString(", "))
          case Right(image) => image
        }
      }
      case Some(existing) => {
        existing.name == name match {
          case true => existing
          case false => update(createdBy, existing, form) match {
            case Left(errors) => sys.error(errors.mkString(", "))
            case Right(image) => image
          }
        }
      }
    }
  }

  def create(createdBy: UserReference, form: ImageForm): Either[Seq[String], Image] = {
    validate(createdBy, form) match {
      case Nil => {

        DB.withConnection { implicit c =>
          SQL(UpsertQuery).on(
            'id -> io.flow.play.util.IdGenerator("img").randomId(),
            'build_id -> form.buildId,
            'name -> form.name.trim,
            'version -> form.version.trim,
            'sort_key -> Util.generateVersionSortKey(form.version.trim),
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        val image = ImagesDao.findByBuildIdAndVersion(form.buildId, form.version).getOrElse {
          sys.error(s"Failed to create image for buildId[${form.buildId}] version[${form.version}]")
        }

        mainActor ! MainActor.Messages.ImageCreated(form.buildId, image.id, form.version.trim)

        Right(image)
      }
      case errors => Left(errors)
    }
  }

  private[this] def update(createdBy: UserReference, image: Image, form: ImageForm): Either[Seq[String], Image] = {
    validate(createdBy, form, Some(image)) match {
      case Nil => {
        DB.withConnection { implicit c =>
          SQL(UpdateQuery).on(
            'id -> image.id,
            'build_id -> form.buildId,
            'name -> form.name.trim,
            'version -> form.version.trim,
            'sort_key -> Util.generateVersionSortKey(form.version.trim),
            'updated_by_user_id -> createdBy.id
          ).execute()
        }

        mainActor ! MainActor.Messages.ImageCreated(form.buildId, image.id, form.version.trim)

        Right(
          ImagesDao.findById(image.id).getOrElse {
            sys.error("Failed to create image")
          }
        )
      }
      case errors => {
        Left(errors)
      }
    }
  }

  def delete(deletedBy: UserReference, image: Image) {
    Delete.delete("images", deletedBy.id, image.id)
  }

}
