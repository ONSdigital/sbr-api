package controllers

import org.scalatest.{FreeSpec, Matchers}
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import play.api.test.Helpers._

/*
 * This just tests the action in this controller.
 * RoutingSpecs are required to test that the Play router is configured to validate arguments and route requests
 * accordingly.
 */
class BadRequestControllerSpec extends FreeSpec with Matchers {

  private trait Fixture extends StubControllerComponentsFactory {
    val UnusedArgument = "unused"

    val controller = new BadRequestController(stubControllerComponents())
  }

  "A request" - {
    "receives a BAD REQUEST response" in new Fixture {
      val action = controller.badRequest(UnusedArgument, UnusedArgument)
      val response = action.apply(FakeRequest())

      status(response) shouldBe BAD_REQUEST
    }
  }
}
