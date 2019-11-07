package controllers

import io.flow.common.v0.models.{Environment, UserReference}
import io.flow.delta.v0.Client
import io.flow.play.util.{AuthHeaders, FlowSession}
import io.flow.test.utils.{FlowMockClient, FlowPlaySpec}
import io.flow.util.Constants

trait MockClient extends FlowPlaySpec with db.Helpers with FlowMockClient[
    io.flow.delta.v0.Client,
    io.flow.delta.v0.errors.GenericErrorResponse,
    io.flow.delta.v0.errors.UnitResponse
  ] {

  override def createAnonymousClient(baseUrl: String): Client = new Client(wsClient, s"http://localhost:$port")
  override def createIdentifiedClient(baseUrl: String, user: UserReference, org: Option[String], session: Option[FlowSession]): Client = {
    val auth = org match {
      case None =>  AuthHeaders.user(user, session = session)
      case Some(o) => AuthHeaders.organization(user, o, environment = Environment.Sandbox, session = session)
    }

    new Client(
      ws = wsClient,
      baseUrl = baseUrl,
      defaultHeaders = authHeaders.headers(auth)
    )
  }

  def identifiedClientSystemUser() = createIdentifiedClient(
    s"http://localhost:$port",
    Constants.SystemUser
  )
}
