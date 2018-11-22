package support

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.FakeRequest
import play.api.test.Helpers._

trait TestUtils extends PlaySpec with GuiceOneAppPerSuite {
  protected[this] def fakeRequest(url: String, method: String = GET) =
    route(app, FakeRequest(method, url)).getOrElse(sys.error(s"Route $url does not exist"))

  protected def getValue(json: Option[String]): String = json match {
    case Some(x: String) => s"$x"
    case _ => sys.error("No Value failed. Forcing test failure")
  }
}
