package io.flow.delta.api.lib

import io.flow.common.v0.models.Name
import io.flow.test.utils.FlowPlaySpec

class GithubUtilSpec extends FlowPlaySpec with db.Helpers {

  "githubHelper.parseName" in {
    githubHelper.parseName("") must be(Name())
    githubHelper.parseName("  ") must be(Name())
    githubHelper.parseName("mike") must be(Name(first = Some("mike")))
    githubHelper.parseName("mike bryzek") must be(Name(first = Some("mike"), last = Some("bryzek")))
    githubHelper.parseName("   mike    maciej    bryzek  ") must be(
      Name(first = Some("mike"), last = Some("maciej bryzek"))
    )
  }

  "parseUri" in {
    GithubUtil.parseUri("http://github.com/mbryzek/apidoc") must be(
      Right(
        Repo("mbryzek", "apidoc")
      )
    )
  }

  "parseUri for invalid URLs" in {
    GithubUtil.parseUri("   ") must be(
      Left(s"URI cannot be an empty string")
    )

    GithubUtil.parseUri("http://github.com") must be(
      Left("URI path cannot be empty for uri[http://github.com]")
    )

    GithubUtil.parseUri("http://github.com/mbryzek") must be(
      Left("Invalid uri path[http://github.com/mbryzek] missing project name")
    )

    GithubUtil.parseUri("http://github.com/mbryzek/apidoc/other") must be(
      Left("Invalid uri path[http://github.com/mbryzek/apidoc/other] - desired exactly two path components")
    )
  }

}
