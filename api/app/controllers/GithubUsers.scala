package controllers

import io.flow.common.v0.models.json._
import io.flow.delta.api.lib.Github
import io.flow.delta.v0.models.GithubAuthenticationForm
import io.flow.delta.v0.models.json._
import io.flow.play.controllers.{FlowController, FlowControllerComponents}
import io.flow.play.util.Validation
import play.api.libs.json._
import play.api.mvc._
import io.flow.error.v0.models.json._

import scala.concurrent.Future

class GithubUsers @javax.inject.Inject() (
  val github: Github,
  val controllerComponents: ControllerComponents,
  val flowControllerComponents: FlowControllerComponents
) extends FlowController {

  import scala.concurrent.ExecutionContext.Implicits.global

  def postGithub() = Action.async(parse.json) { request =>
    request.body.validate[GithubAuthenticationForm] match {
      case e: JsError => Future {
        UnprocessableEntity(Json.toJson(Validation.invalidJson(e)))
      }
      case s: JsSuccess[GithubAuthenticationForm] => {
        val form = s.get
        github.getUserFromCode(form.code).map {
          case Left(errors) => UnprocessableEntity(Json.toJson(Validation.errors(errors)))
          case Right(user) => Ok(Json.toJson(user))
        }
      }
    }
  }

}

