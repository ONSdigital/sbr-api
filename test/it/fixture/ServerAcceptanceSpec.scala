package fixture

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }

trait ServerAcceptanceSpec extends AcceptanceSpec with GuiceOneServerPerSuite with DefaultAwaitTimeout with FutureAwaits
