package controllers

import io.flow.healthcheck.v0.Client
import io.flow.healthcheck.v0.models.Healthcheck
import io.flow.test.utils.FlowPlaySpec

class HealthchecksSpec extends FlowPlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val client = new Client(wsClient, s"http://localhost:$port")

  "GET /_internal_/healthcheck" in {
    await(client.healthchecks.getHealthcheck()) must be (Healthcheck("healthy"))
  }

}
