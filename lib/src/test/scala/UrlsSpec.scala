package io.flow.delta.lib

import io.flow.delta.v0.models.ItemSummaryUndefinedType
import io.flow.play.util.Config
import org.scalatestplus.play.PlaySpec

class UrlsSpec extends PlaySpec with Factories {

  private[this] lazy val mockConfig = new Config {
    override def optionalString(name: String): Option[String] = {
      name match {
        case "delta.www.host" => Some("http://localhost")
        case other => sys.error(s"Need to mock config variable[$other]")
      }
    }

    override def optionalList(name: String): Option[Seq[String]] = ???

    override def get(name: String): Option[String] = ???
  }

  private[this] lazy val urls = Urls(mockConfig)


  "www" in {
    urls.www("/foo") must be("http://localhost/foo")
  }

  "itemSummary" in {
    val project = makeProjectSummary()
    urls.itemSummary(project) must be(s"/projects/${project.id}")

    urls.itemSummary(ItemSummaryUndefinedType("other")) must be("#")
  }

}
