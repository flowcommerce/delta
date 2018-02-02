package io.flow.delta.lib

import io.flow.play.util.Config
import io.flow.delta.v0.models.ItemSummaryUndefinedType
import org.scalatest.{FunSpec, Matchers}

class UrlsSpec extends FunSpec with Matchers with Factories {

  private[this] lazy val mockConfig = new Config {
    override def optionalString(name: String): Option[String] = {
      name match {
        case "delta.www.host" => Some("http://localhost")
        case other => sys.error(s"Need to mock config variable[$other]")
      }
    }
  }

  private[this] lazy val urls = Urls(mockConfig)


  it("www") {
    urls.www("/foo") should be("http://localhost/foo")
  }

  it("itemSummary") {
    val project = makeProjectSummary()
    urls.itemSummary(project) should be(s"/projects/${project.id}")

    urls.itemSummary(ItemSummaryUndefinedType("other")) should be("#")
  }

}
