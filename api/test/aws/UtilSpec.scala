package io.flow.delta.aws

import org.specs2.mutable._

class UtilSpec extends Specification {

  "parseImage" in {
    Util.parseImage("flow/user:0.0.1") must beEqualTo(Some(Util.DockerImage("flow", "user", "0.0.1")))
    Util.parseImage("flow-commerce/delta-api:0.0.1") must beEqualTo(Some(Util.DockerImage("flow-commerce", "delta-api", "0.0.1")))
    Util.parseImage("flow") must beEqualTo(None)

  }

}
