package config.providers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.NOT_FOUND
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{ BindingKey, bind }
import play.api.libs.ws.{ WSClient, WSRequest, WSResponse }
import repository.DataSourceNames.SbrCtrl
import repository.rest.UnitRepository

import scala.concurrent.Future

class SbrCtrlUnitRepositoryProviderSpec extends FreeSpec with Matchers with MockFactory with GuiceOneAppPerSuite with ScalaFutures with EitherValues {

  private trait Fixture {
    val SomePath = "some-path"
    val wsClient = mock[WSClient]
    val wsRequest = stub[WSRequest]
    val wsResponse = stub[WSResponse]
    val application = new GuiceApplicationBuilder().overrides(bind[WSClient].toInstance(wsClient)).build()

    (wsRequest.withHeaders _).when(*).returns(wsRequest)
    (wsRequest.get _).when().returns(Future.successful(wsResponse))
    (wsResponse.status _).when().returns(NOT_FOUND)
  }

  /*
   * The base url used by the provided repository should consist of the protocol / host / port configured at
   * api.sbr.control in the application.conf
   */
  "A SbrCtrlUnitRepositoryProvider should provide a unit repository injected with the SbrCtrl base url" in new Fixture {
    (wsClient.url _).expects(s"http://localhost:9001/$SomePath").returning(wsRequest)

    val unitRepository = application.injector.instanceOf(BindingKey(classOf[UnitRepository]).qualifiedWith(SbrCtrl))

    whenReady(unitRepository.getJson(SomePath)) { result =>
      result.right.value shouldBe empty
    }
  }
}
