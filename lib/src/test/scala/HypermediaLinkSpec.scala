package io.flow.delta.lib

import org.scalatestplus.play.PlaySpec

class HypermediaLinkSpec extends PlaySpec {

  "parse valid links" in {
    HypermediaLink.parse("<https://api.github.com/user/repos?page=3&per_page=100>; rel=\"next\"") must equal(
      Right(
        HypermediaLink(
          "https://api.github.com/user/repos?page=3&per_page=100",
          HypermediaLinkRel.Next,
        )
      )
    )
  }

  "parse all rel values" in {
    def test(value: String) = {
      HypermediaLink.parse(s"""<https://api.github.com/user/repos?page=3&per_page=100>; rel="$value"""") match {
        case Left(errors) => sys.error(s"Error: $errors")
        case Right(v) => v.rel
      }
    }
    test("next") must equal(HypermediaLinkRel.Next)
    test(" next ") must equal(HypermediaLinkRel.Next)
    test(" NEXT ") must equal(HypermediaLinkRel.Next)
    test("first") must equal(HypermediaLinkRel.First)
    test("last") must equal(HypermediaLinkRel.Last)
    test("previous") must equal(HypermediaLinkRel.Previous)
    test("prev") must equal(HypermediaLinkRel.Previous)
    test("foo") must equal(HypermediaLinkRel.Other("foo"))
  }

  "invalid links" in {
    HypermediaLink.parse("<https://api.github.com/user/repos?page=3&per_page=100>") must equal(
      Left(Seq("Cannot parse link value '<https://api.github.com/user/repos?page=3&per_page=100>': expected a single semi colon"))
    )
  }

  "invalid url" in {
    HypermediaLink.parse("https://api.github.com; rel=\"next\"") must equal(
      Left(Seq("Cannot parse link url 'https://api.github.com': Must start with '<' and end with '>'"))
    )
  }

  "invalid rel" in {
    HypermediaLink.parse("<https://api.github.com/user/repos?page=3&per_page=100>; next") must equal(
      Left(Seq("Cannot parse link rel 'next': Must start with rel="))
    )
  }
}
