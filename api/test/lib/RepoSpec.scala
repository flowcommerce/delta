package io.flow.delta.api.lib

import org.scalatestplus.play.PlaySpec

class RepoSpec extends PlaySpec {

  "toString" in {
    Repo("mbryzek", "apidoc").toString must be("mbryzek/apidoc")
  }

}
