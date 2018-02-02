package io.flow.delta.api.lib

import io.flow.common.v0.models.Name
import org.specs2.mutable._

class RepoSpec extends Specification {

  "toString" in {
    Repo("mbryzek", "apidoc").toString must beEqualTo("mbryzek/apidoc")
  }

}
