package repository.admindata

import java.time.Month.APRIL

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsError, JsSuccess, Json, Reads }
import repository.rest.Repository
import support.sample.SampleVat
import tracing.TraceData
import uk.gov.ons.sbr.models.{ AdminData, Period, UnitId, VatRef }

import scala.concurrent.Future

class RestAdminDataRepositorySpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    val TargetUnitId = UnitId("123456789012")
    val TargetPeriod = Period.fromYearMonth(2018, APRIL)
    val AdminDataJson = Json.parse(s"""{"some":"value"}""")
    val SampleAdminData = AdminData(
      id = TargetUnitId,
      period = TargetPeriod,
      variables = SampleVat.asJson(VatRef(TargetUnitId.value))
    )
    private val AdminDataType = "someAdminDataType"
    val SpanName = s"get-admin-data-$AdminDataType"

    val traceData = stub[TraceData]
    val unitRepository = mock[Repository]
    val readsAdminData = mock[Reads[AdminData]]
    val adminDataRepository = new RestAdminDataRepository(unitRepository, readsAdminData, AdminDataType)
  }

  "An AdminData repository" - {
    "returns the requested admin data when found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/records/${TargetUnitId.value}/periods/${Period.asString(TargetPeriod)}", SpanName, traceData).returning(
        Future.successful(Right(Some(AdminDataJson)))
      )
      (readsAdminData.reads _).expects(AdminDataJson).returning(JsSuccess(SampleAdminData))

      whenReady(adminDataRepository.retrieveAdminData(TargetUnitId, TargetPeriod, traceData)) { result =>
        result.right.value shouldBe Some(SampleAdminData)
      }
    }

    "returns nothing when the requested admin data cannot be found" in new Fixture {
      (unitRepository.getJson _).expects(s"v1/records/${TargetUnitId.value}/periods/${Period.asString(TargetPeriod)}", SpanName, traceData).returning(
        Future.successful(Right(None))
      )

      whenReady(adminDataRepository.retrieveAdminData(TargetUnitId, TargetPeriod, traceData)) { result =>
        result.right.value shouldBe empty
      }
    }

    "returns an error message" - {
      "when the retrieval fails" in new Fixture {
        val failureMessage = "some failure message"
        (unitRepository.getJson _).expects(s"v1/records/${TargetUnitId.value}/periods/${Period.asString(TargetPeriod)}", SpanName, traceData).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(adminDataRepository.retrieveAdminData(TargetUnitId, TargetPeriod, traceData)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when unable to parse the json as an AdminData model" in new Fixture {
        (unitRepository.getJson _).expects(s"v1/records/${TargetUnitId.value}/periods/${Period.asString(TargetPeriod)}", SpanName, traceData).returning(
          Future.successful(Right(Some(AdminDataJson)))
        )
        (readsAdminData.reads _).expects(AdminDataJson).returning(JsError("unexpected json format"))

        whenReady(adminDataRepository.retrieveAdminData(TargetUnitId, TargetPeriod, traceData)) { result =>
          result.left.value should startWith("Unable to parse json response")
        }
      }
    }
  }
}
