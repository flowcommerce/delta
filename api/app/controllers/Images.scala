package controllers

import db.{ImagesDao, ImagesWriteDao}
import io.flow.delta.v0.models.Image
import io.flow.delta.v0.models.json._
import io.flow.play.controllers.FlowControllerComponents
import play.api.libs.json._
import play.api.mvc._

@javax.inject.Singleton
class Images @javax.inject.Inject() (
  helpers: Helpers,
  imagesDao: ImagesDao,
  imagesWriteDao: ImagesWriteDao,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends BaseIdentifiedRestController {

  def get(
    id: Option[Seq[String]],
    build: Option[String],
    name: Option[String],
    limit: Long,
    offset: Long,
    sort: String
  ) = Identified {
    helpers.withOrderBy(sort) { orderBy =>
      Ok(
        Json.toJson(
          imagesDao.findAll(
            ids = optionals(id),
            buildId = build,
            names = name.map { n => Seq(n) },
            limit = Some(limit),
            offset = offset,
            orderBy = orderBy
          )
        )
      )
    }
  }

  def getById(id: String) = Identified {
    withImage(id) { image =>
      Ok(Json.toJson(image))
    }
  }

  def deleteById(id: String) = Identified { request =>
    withImage(id) { image =>
      imagesWriteDao.delete(request.user, image)
      NoContent
    }
  }

  private def withImage(id: String)(
    f: Image => Result
  ): Result = {
    imagesDao.findById(id) match {
      case None => {
        Results.NotFound
      }
      case Some(image) => {
        f(image)
      }
    }
  }

}
