package io.flow.delta.www.lib

import io.flow.play.util
import io.flow.common.v0.models.UserReference
import io.flow.delta.v0.{Authorization, Client}
import io.flow.token.v0.Tokens

trait DeltaClientProvider extends io.flow.token.v0.interfaces.Client {

  def newClient(user: Option[UserReference]): Client

}

@javax.inject.Singleton
class DefaultDeltaClientProvider @javax.inject.Inject() (
  config: util.Config
) extends DeltaClientProvider {

  private[this] val host = config.requiredString("delta.api.host")

  private[this] lazy val anonymousClient = new Client(host)

  override def newClient(user: Option[UserReference]): Client = {
    user match {
      case None => {
        anonymousClient
      }
      case Some(u) => {
        new Client(
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

  override def validations = throw new UnsupportedOperationException()
}
