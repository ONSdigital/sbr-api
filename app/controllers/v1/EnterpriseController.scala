package controllers.v1

import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ Json, Writes }
import play.api.mvc.{ Action, AnyContent, Controller, Result }
import services.EnterpriseService
import services.EnterpriseService.ErrorMessage
import uk.gov.ons.sbr.models.{ Ern, LinkedUnit, Period }

/*
 * Note that we are relying on regex patterns in the routes definitions to apply argument validation.
 * Only requests with valid arguments should be routed to the retrieve... actions.
 * All other requests should be routed to the badRequest action.
 */
@Api("Enterprise")
@Singleton
class EnterpriseController @Inject() (enterpriseService: EnterpriseService, writesLinkedUnit: Writes[LinkedUnit]) extends Controller {
  @ApiOperation(
    value = "Json representation of the enterprise along with its links to other units",
    notes = "children represents a mapping from each child's unique identifier to the associated unitType; " +
    "vars simply passes through the representation of the unit received from the control API (and so is not defined here)",
    response = classOf[LinkedUnit],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "Either an enterprise could not be found with the specified ERN and Period, or no related unit links are defined"),
    new ApiResponse(code = 500, message = "The attempt to retrieve the enterprise could not complete due to an unrecoverable error"),
    new ApiResponse(code = 504, message = "A response was not received from a data server within the required time interval")
  ))
  def retrieveEnterprise(
    @ApiParam(value = "Period (unit load date) - in YYYYMM format", example = "201803", required = true) periodStr: String,
    @ApiParam(value = "Enterprise Reference Number (ERN) - a ten digit number", example = "1000000012", required = true) ernStr: String
  ): Action[AnyContent] = Action.async {
    enterpriseService.retrieve(Period.fromString(periodStr), Ern(ernStr)).map { errorOrLinkedUnit =>
      errorOrLinkedUnit.fold(resultOnFailure, resultOnSuccess)
    }
  }

  private def resultOnFailure(errorMessage: ErrorMessage): Result =
    errorMessage match {
      case _ if errorMessage.startsWith("Timeout") => GatewayTimeout
      case _ => InternalServerError
    }

  private def resultOnSuccess(optLinkedUnit: Option[LinkedUnit]): Result =
    optLinkedUnit.fold[Result](NotFound) { linkedUnit =>
      Ok(Json.toJson(linkedUnit)(writesLinkedUnit))
    }

  def badRequest(periodStr: String, ernStr: String) = Action {
    BadRequest
  }
}
