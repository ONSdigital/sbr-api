package services.admindata

import java.time.Month.FEBRUARY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ EitherValues, FreeSpec, Matchers }
import play.api.libs.json.{ JsObject, Json }
import repository.{ AdminDataRepository, UnitLinksRepository }
import uk.gov.ons.sbr.models._
import unitref.UnitRef

import scala.concurrent.Future

class AdminDataServiceSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures with EitherValues {

  private trait Fixture {
    case class FakeUnitRef(value: String)

    val TargetUnitRef = FakeUnitRef("346942023239")
    val TargetUnitId = UnitId(TargetUnitRef.value)
    // we are not actually explicitly testing VAT here, but this needs to be one of the sealed unit types
    val TargetUnitType = UnitType.ValueAddedTax
    val TargetPeriod = Period.fromYearMonth(2018, FEBRUARY)
    val ParentLinks: Option[Map[UnitType, UnitId]] = Some(Map(UnitType.Enterprise -> UnitId("1122334455")))
    val AdminDataVars = Json.parse(s"""{"field1":"value2", "field2":42, "field3": {"sub-field":"sub-value"}}""").as[JsObject]

    val unitRefType = stub[UnitRef[FakeUnitRef]]
    val unitLinksRepository = mock[UnitLinksRepository]
    val adminDataRepository = mock[AdminDataRepository]
    val service = new AdminDataService(unitRefType, unitLinksRepository, adminDataRepository)

    def unitLinks(withUnitId: UnitId, withPeriod: Period, withParents: Option[Map[UnitType, UnitId]]): UnitLinks =
      UnitLinks(withUnitId, TargetUnitType, withPeriod, parents = withParents, children = None)

    def adminData(withUnitId: UnitId, withPeriod: Period, withVariables: JsObject): AdminData =
      AdminData(withUnitId, withPeriod, withVariables)
  }

  "An AdminData Service" - {
    "assembles an admin data unit with its associated links" - {
      "when both the unit link and admin data are found for the target unit reference and period" in new Fixture {
        (unitRefType.toUnitId _).when(TargetUnitRef).returns(TargetUnitId)
        (unitRefType.toIdTypePair _).when(TargetUnitRef).returns(TargetUnitId -> TargetUnitType)
        (unitRefType.toUnitId _).when(TargetUnitRef).returns(TargetUnitId)
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod).returning(
          Future.successful(Right(Some(unitLinks(withUnitId = TargetUnitId, withPeriod = TargetPeriod, withParents = ParentLinks))))
        )
        (adminDataRepository.retrieveAdminData _).expects(TargetUnitId, TargetPeriod).returning(
          Future.successful(Right(Some(adminData(withUnitId = TargetUnitId, withPeriod = TargetPeriod, withVariables = AdminDataVars))))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef)) { result =>
          result.right.value shouldBe Some(LinkedUnit(
            TargetUnitId,
            TargetUnitType,
            TargetPeriod,
            parents = ParentLinks,
            children = None,
            vars = AdminDataVars
          ))
        }
      }
    }

    "returns nothing" - {
      "when a unit links entry for the target unit cannot be found" in new Fixture {
        (unitRefType.toIdTypePair _).when(TargetUnitRef).returns(TargetUnitId -> TargetUnitType)
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef)) { result =>
          result.right.value shouldBe empty
        }
      }

      "when the unit admin data cannot be found" in new Fixture {
        (unitRefType.toIdTypePair _).when(TargetUnitRef).returns(TargetUnitId -> TargetUnitType)
        (unitRefType.toUnitId _).when(TargetUnitRef).returns(TargetUnitId)
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod).returning(
          Future.successful(Right(Some(unitLinks(withUnitId = TargetUnitId, withPeriod = TargetPeriod, withParents = ParentLinks))))
        )
        (adminDataRepository.retrieveAdminData _).expects(TargetUnitId, TargetPeriod).returning(
          Future.successful(Right(None))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef)) { result =>
          result.right.value shouldBe empty
        }
      }
    }

    "returns an error message" - {
      "when a unit links retrieval fails" in new Fixture {
        val failureMessage = "unitLinks retrieval failure"
        (unitRefType.toIdTypePair _).when(TargetUnitRef).returns(TargetUnitId -> TargetUnitType)
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef)) { result =>
          result.left.value shouldBe failureMessage
        }
      }

      "when a unit admin data retrieval fails" in new Fixture {
        val failureMessage = "VAT admin data retrieval failed"
        (unitRefType.toIdTypePair _).when(TargetUnitRef).returns(TargetUnitId -> TargetUnitType)
        (unitRefType.toUnitId _).when(TargetUnitRef).returns(TargetUnitId)
        (unitLinksRepository.retrieveUnitLinks _).expects(TargetUnitId, TargetUnitType, TargetPeriod).returning(
          Future.successful(Right(Some(unitLinks(withUnitId = TargetUnitId, withPeriod = TargetPeriod, withParents = ParentLinks))))
        )
        (adminDataRepository.retrieveAdminData _).expects(TargetUnitId, TargetPeriod).returning(
          Future.successful(Left(failureMessage))
        )

        whenReady(service.retrieve(TargetPeriod, TargetUnitRef)) { result =>
          result.left.value shouldBe failureMessage
        }
      }
    }
  }
}
