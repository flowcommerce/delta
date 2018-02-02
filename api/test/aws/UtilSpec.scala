package io.flow.delta.aws

import org.scalatestplus.play.PlaySpec

class UtilSpec extends PlaySpec {

  "parseImage" in {
    Util.parseImage("flow/user:0.0.1") must be(Some(Util.DockerImage("flow", "user", "0.0.1")))
    Util.parseImage("flow-commerce/delta-api:0.0.1") must be(Some(Util.DockerImage("flow-commerce", "delta-api", "0.0.1")))
    Util.parseImage("flow") must be(None)

  }

}
