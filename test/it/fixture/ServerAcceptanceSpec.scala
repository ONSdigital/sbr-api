package fixture

import org.scalatestplus.play.guice.GuiceOneServerPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }

trait ServerAcceptanceSpec extends AcceptanceSpec with GuiceOneServerPerTest with DefaultAwaitTimeout with FutureAwaits {
  /*
   * A common acceptance scenario is failure handling, and a simple approach to triggering failure conditions is
   * to shut down the wiremock server that represents some collaborating service.
   * For some currently unknown reason, this can result in TimeoutExceptions rather than ConnectExceptions.  In the
   * timeout case, the connection waits for the full timeout period before failing.
   * We therefore artificially configure a shorter timeout here, to help speed up such acceptance tests.
   */
  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(Map("play.ws.timeout.connection" -> "2500ms")).build()
}
