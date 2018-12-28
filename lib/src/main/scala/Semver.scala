package io.flow.delta.lib

case class Semver(major: Int, minor: Int, micro: Int) extends Ordered[Semver] {

  val label = {
    Seq(major, minor, micro).mkString(".")
  }

  val sortKey: String = "%s.%s.%s".format(Semver.Pad + major, Semver.Pad + minor, Semver.Pad + micro)
  
  def next(): Semver = {
    if (micro >= 99) {
      Semver(major, minor + 1, 0)
    } else {
      Semver(major, minor, micro + 1)
    }
  }

  def compare(other: Semver) = {
    sortKey.compare(other.sortKey)
  }

}

object Semver {

  val Pad = 10000
  private[this] val Pattern = """^(\d+)\.(\d+)\.(\d+)$""".r

  def parse(value: String): Option[Semver] = {
    value match {
      case Pattern(major, minor, micro) => Some(Semver(major.toInt, minor.toInt, micro.toInt))
      case _ => None
    }
  }

  def isSemver(value: String): Boolean = {
    value match {
      case Pattern(_, _, _) => true
      case _ => false
    }
  }
}

