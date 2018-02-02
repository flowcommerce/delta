package io.flow.delta.lib

import org.scalatestplus.play.PlaySpec

class SemverSpec extends PlaySpec {

  "Semver.parse" in {
    Semver.parse("foo") must be(None)
    Semver.parse("0") must be(None)
    Semver.parse("0.1") must be(None)
    Semver.parse("0.1.2") must be(Some(Semver(0,1,2)))
  }

  "Semver.isSemver" in {
    Semver.isSemver("foo") must be(false)
    Semver.isSemver("0") must be(false)
    Semver.isSemver("0.1") must be(false)
    Semver.isSemver("0.1.2") must be(true)
  }

  "next" in {
    Semver(0, 1, 2).next must be(Semver(0, 1, 3))
  }

  "next prevents minor from exceeded 100" in {
    Semver(0, 1, 98).next must be(Semver(0, 1, 99))
    Semver(0, 1, 99).next must be(Semver(0, 2, 0))
  }

  "label" in {
    Semver(0, 1, 2).label must be("0.1.2")
  }

  "sortKey" in {
    Seq(
      Semver(0, 1, 2),
      Semver(0, 1, 10),
      Semver(20, 9, 0)
    ).sorted.map(_.label) must be(
      Seq("0.1.2", "0.1.10", "20.9.0")
    )
  }

}
