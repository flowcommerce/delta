package controllers

import io.flow.postgresql.Authorization
import io.flow.play.controllers.FlowController

trait BaseFlowController extends FlowController with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  def authorization[T](request: IdentifiedRequest[T]): Authorization = {
    Authorization.User(request.user.id)
  }

}
