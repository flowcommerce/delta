package controllers

import io.flow.test.utils.FlowPlaySpec
import io.flow.usage.util.UsageUtil
import io.flow.usage.v0.Client
import io.flow.usage.v0.models.json._
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class UsageSpec extends FlowPlaySpec {
  private[this] def uu = app.injector.instanceOf[UsageUtil]

  import scala.concurrent.ExecutionContext.Implicits.global

  "Check usage"  in {
    val j = Json.toJson(uu.currentUsage)
    println(s"Found API Usage: $j" )
    val r = Json.toJson(
      Await.result(
        new Client(
          wsClient,
          s"http://localhost:$port"
        ).Usages.getUsage(), 3 seconds
      )
    )

    j must be(r)
  }
}

