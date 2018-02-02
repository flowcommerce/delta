package io.flow.delta.api.lib

import io.flow.delta.v0.models.Version
import org.specs2.mutable._

class StateDiffSpec extends Specification {

  "Nil" in {
    StateDiff.up(Nil, Nil) must beEqualTo(Nil)
    StateDiff.down(Nil, Nil) must beEqualTo(Nil)
  }

  "no change" in {
    val last = Seq(Version("0.0.1", 2), Version("0.0.2", 3))
    StateDiff.up(last, last) must beEqualTo(Nil)
    StateDiff.down(last, last) must beEqualTo(Nil)
  }

  "bring up one instance" in {
    val last = Seq(Version("0.0.1", 2))
    val desired = Seq(Version("0.0.1", 3))
    StateDiff.up(last, desired) must beEqualTo(Seq(StateDiff("0.0.1", 2, 3)))
    StateDiff.down(last, desired) must beEqualTo(Nil)
  }

  "bring up one instance w/ multiple versions" in {
    val last = Seq(Version("0.0.1", 2), Version("0.0.2", 2))
    val desired = Seq(Version("0.0.1", 2), Version("0.0.2", 3), Version("0.0.3", 1))
    StateDiff.up(last, desired) must beEqualTo(Seq(StateDiff("0.0.2", 2, 3), StateDiff("0.0.3", 0, 1)))
    StateDiff.down(last, desired) must beEqualTo(Nil)
  }

  "bring down one instance" in {
    val last = Seq(Version("0.0.1", 2))
    val desired = Seq(Version("0.0.1", 1))
    StateDiff.up(last, desired) must beEqualTo(Nil)
    StateDiff.down(last, desired) must beEqualTo(Seq(StateDiff("0.0.1", 2, 1)))
  }

  "bring down one instance w/ multiple versions" in {
    val last = Seq(Version("0.0.1", 2), Version("0.0.2", 2))
    val desired = Seq(Version("0.0.1", 1), Version("0.0.2", 1))
    StateDiff.up(last, desired) must beEqualTo(Nil)
    StateDiff.down(last, desired) must beEqualTo(Seq(StateDiff("0.0.1", 2, 1), StateDiff("0.0.2", 2, 1)))
  }

  "upgrade version" in {
    val last = Seq(Version("0.0.1", 2))
    val desired = Seq(Version("0.0.2", 2))
    StateDiff.up(last, desired) must beEqualTo(Seq(StateDiff("0.0.2", 0, 2)))
    StateDiff.down(last, desired) must beEqualTo(Seq(StateDiff("0.0.1", 2, 0)))
  }

  "downgrade version" in {
    val last = Seq(Version("0.0.2", 2))
    val desired = Seq(Version("0.0.1", 2))
    StateDiff.up(last, desired) must beEqualTo(Seq(StateDiff("0.0.1", 0, 2)))
    StateDiff.down(last, desired) must beEqualTo(Seq(StateDiff("0.0.2", 2, 0)))
  }

}
