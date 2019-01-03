import io.flow.play.clients.Registry
import play.api.libs.ws.WSClient

@javax.inject.Singleton
class DefaultTokenClient @javax.inject.Inject() (registry: Registry, wsClient: WSClient) extends io.flow.token.v0.Client(ws= wsClient, baseUrl = registry.host("catalog"))
