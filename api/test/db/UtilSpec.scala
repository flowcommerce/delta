package db

import org.scalatest._
import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

class UtilSpec extends PlaySpec with OneAppPerSuite with Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "generateVersionSortKey" in {
    Util.generateVersionSortKey("0.0.1") must be("6:10000.10000.10001")
    Util.generateVersionSortKey("0") must be("3:0")
    Util.generateVersionSortKey("other") must be("3:other")

    Seq("0.0.10", "0.0.5", "1.0.0", "other").sortBy { Util.generateVersionSortKey(_) } must be(
      Seq("other", "0.0.5", "0.0.10", "1.0.0")
    )
  }

}
