package services.finder

import java.time.Month.JUNE

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsObject, Json }
import repository.AdminDataRepository
import uk.gov.ons.sbr.models.UnitType.{ LegalUnit, ValueAddedTax }
import uk.gov.ons.sbr.models._
import unitref.UnitRef

import scala.concurrent.Future

class AdminDataFinderSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    case class FakeAdminDataRef(value: String)

    val TargetPeriod = Period.fromYearMonth(2018, JUNE)
    val TargetUnitRef = FakeAdminDataRef("1234")
    val TargetUnitId = UnitId(TargetUnitRef.value)
    val TargetUnitType = ValueAddedTax // this test is not VAT specific - we just a valid AdminData unit type
    val AdminDataUnitLinks = UnitLinks(
      id = TargetUnitId,
      unitType = TargetUnitType,
      period = TargetPeriod,
      parents = Some(Map(LegalUnit -> UnitId("1234567890123456"))),
      children = None
    )
    val TheAdminData = AdminData(
      id = TargetUnitId,
      period = TargetPeriod,
      variables = Json.parse(s"""{"fake":"json"}""").as[JsObject]
    )

    val unitRefType = stub[UnitRef[FakeAdminDataRef]]
    (unitRefType.toIdTypePair _).when(TargetUnitRef).returns(TargetUnitId -> TargetUnitType)
    val adminDatarepository = mock[AdminDataRepository]
    val finder = new AdminDataFinder[FakeAdminDataRef](unitRefType, adminDatarepository)
  }

  "An AdminData finder" - {
    "retrieves the admin data via the repository" - {
      "returning the admin data variables when the admin data is found" in new Fixture {
        (adminDatarepository.retrieveAdminData _).expects(TargetUnitId, TargetPeriod).returning(
          Future.successful(Right(Some(TheAdminData)))
        )

        whenReady(finder.find(TargetPeriod, TargetUnitRef, AdminDataUnitLinks)) { result =>
          result.right.value shouldBe Some(TheAdminData.variables)
        }
      }

      "returning nothing when the admin data is not found" in new Fixture {
        (adminDatarepository.retrieveAdminData _).expects(TargetUnitId, TargetPeriod).returning(
          Future.successful(Right(None))
        )

        whenReady(finder.find(TargetPeriod, TargetUnitRef, AdminDataUnitLinks)) { result =>
          result.right.value shouldBe empty
        }
      }

      "returning the failure message when the retrieval fails" in new Fixture {
        val failureMessage = "retrieval failed"
        (adminDatarepository.retrieveAdminData _).expects(TargetUnitId, TargetPeriod).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(finder.find(TargetPeriod, TargetUnitRef, AdminDataUnitLinks)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }
  }
}
