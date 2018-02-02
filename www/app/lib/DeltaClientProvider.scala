package io.flow.delta.www.lib

import io.flow.play.util
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.{Authorization, Client}
import io.flow.token.v0.Tokens
import play.api.libs.ws.WSClient

trait DeltaClientProvider extends io.flow.token.v0.interfaces.Client {

  def newClient(user: Option[UserReference]): Client

}

@javax.inject.Singleton
class DefaultDeltaClientProvider @javax.inject.Inject() (
  config: util.Config,
  wSClient: WSClient
) extends DeltaClientProvider {

  private[this] val host = config.requiredString("delta.api.host")

  private[this] lazy val anonymousClient = new Client(ws = wSClient, host)

  override def newClient(user: Option[UserReference]): Client = {
    user match {
      case None => {
        anonymousClient
      }
      case Some(u) => {
        new Client(
          ws = wSClient,
          baseUrl = host,
          auth = Some(
            Authorization.Basic(
              username = u.id.toString,
              password = None
            )
          )
        )
      }
    }
  }

  override def baseUrl: String = throw new UnsupportedOperationException()

  override def tokens: Tokens = throw new UnsupportedOperationException()

  override def tokenValidations = throw new UnsupportedOperationException()

  override def organizationTokens = throw new UnsupportedOperationException()

  override def partnerTokens = throw new UnsupportedOperationException()
}
