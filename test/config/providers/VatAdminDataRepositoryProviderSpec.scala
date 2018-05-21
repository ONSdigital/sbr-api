package config.providers

import java.time.Month.JANUARY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.NOT_FOUND
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{ BindingKey, bind }
import play.api.libs.ws.{ WSClient, WSRequest, WSResponse }
import repository.AdminDataRepository
import repository.DataSourceNames.Vat
import uk.gov.ons.sbr.models.{ Period, UnitId }

import scala.concurrent.Future

class VatAdminDataRepositoryProviderSpec extends FreeSpec with Matchers with MockFactory with GuiceOneAppPerSuite with ScalaFutures with EitherValues {

  private trait Fixture {
    val SomeUnitId = UnitId("1234")
    val SomePeriod = Period.fromYearMonth(2018, JANUARY)

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
   * api.admin.data.vat in the application.conf
   */
  "A VatAdminDataRepositoryProvider should provide an admin data repository injected with the VAT admin data base url" in new Fixture {
    (wsClient.url _).expects(s"http://localhost:9005/v1/records/${SomeUnitId.value}/periods/${Period.asString(SomePeriod)}").returning(wsRequest)

    val adminDataRepository = application.injector.instanceOf(BindingKey(classOf[AdminDataRepository]).qualifiedWith(Vat))

    whenReady(adminDataRepository.retrieveAdminData(SomeUnitId, SomePeriod)) { result =>
      result.right.value shouldBe empty
    }
  }
}
