package support

import org.scalatest.Outcome
import play.api.http.Port
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

/*
 * Note that a limitation of WsTestClient is that it appears to create a WSClient that has a default configuration.
 * Any custom timeout settings in application.conf for example are ignored.
 *
 * If you need a fully configured instance of WSClient, instead mix in one of the GuiceOneAppPer... traits, and use
 * the application injector to obtain an instance via app.injector.instanceOf[WSClient].
 */
trait WsClientFixture extends org.scalatest.fixture.TestSuite {
  override type FixtureParam = WSClient

  def wsPort: Port

  override protected def withFixture(test: OneArgTest): Outcome = {
    WsTestClient.withClient { wsClient =>
      withFixture(test.toNoArgTest(wsClient))
    }(wsPort)
  }
}
