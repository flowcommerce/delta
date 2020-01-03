package io.flow.delta.lib

import org.scalatestplus.play.PlaySpec

class HypermediaLinksSpec extends PlaySpec {

  "parse valid links" in {
    HypermediaLinks.parse("<https://api.github.com>; rel=\"next\"") must equal(
      Right(
        HypermediaLinks(Seq(
          HypermediaLink("https://api.github.com", HypermediaLinkRel.Next)
        ))
      )
    )

    HypermediaLinks.parse("<https://api.github.com?page=2>; rel=\"next\", <https://api.github.com>; rel=\"prev\"") must equal(
      Right(
        HypermediaLinks(Seq(
          HypermediaLink("https://api.github.com?page=2", HypermediaLinkRel.Next),
          HypermediaLink("https://api.github.com", HypermediaLinkRel.Previous)
        ))
      )
    )
  }


  "parse invalid links" in {
    HypermediaLinks.parse("foo") must equal(
      Left(Seq("Cannot parse link value 'foo': expected a single semi colon"))
    )
  }
}
