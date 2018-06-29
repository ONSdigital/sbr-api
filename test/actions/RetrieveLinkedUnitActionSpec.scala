package actions

import java.time.Month.MAY

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FreeSpec, Matchers }
import play.api.mvc.Results.NotFound
import play.api.mvc.{ AnyContent, Result }
import play.api.test.FakeRequest
import services.LinkedUnitService
import uk.gov.ons.sbr.models.{ Ern, Period }

import scala.concurrent.Future

class RetrieveLinkedUnitActionSpec extends FreeSpec with Matchers with MockFactory with ScalaFutures {

  private trait Fixture {
    type UnitRef = Ern
    val TargetPeriod = Period.fromYearMonth(2018, MAY)
    val TargetUnitRef = Ern("1234567890")
    val ServiceResult = Right(None)
    val ControllerResult = NotFound

    val service = mock[LinkedUnitService[UnitRef]]
    val controllerBlock = mockFunction[LinkedUnitRequest[AnyContent], Result]
    val retrieveLinkedUnitAction = new RetrieveLinkedUnitAction[UnitRef](service)
  }

  "A retrieveLinkedUnitAction" - {
    "invokes the controller block with a request containing the linkedUnit retrieval outcome" in new Fixture {
      (service.retrieve _).expects(TargetPeriod, TargetUnitRef).returns(Future.successful(ServiceResult))
      controllerBlock.expects(where[LinkedUnitRequest[AnyContent]] { lur =>
        lur.linkedUnitResult == ServiceResult
      }).returning(ControllerResult)

      val action = retrieveLinkedUnitAction(TargetPeriod, TargetUnitRef)(controllerBlock)
      whenReady(action(FakeRequest())) { result =>
        result shouldBe ControllerResult
      }
    }
  }
}
