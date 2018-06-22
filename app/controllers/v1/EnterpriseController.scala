package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.JsObject
import play.api.mvc.{ Action, AnyContent, _ }
import uk.gov.ons.sbr.models.{ Ern, Period, UnitId, UnitType }
import unitref.UnitRef

@Api("Enterprise")
@Singleton
class EnterpriseController @Inject() (
    unitRefType: UnitRef[Ern],
    retrieveLinkedUnitAction: LinkedUnitRequestActionBuilderMaker[Ern],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends LinkedUnitController[Ern](unitRefType, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {
  @ApiOperation(
    value = "Json representation of the enterprise along with its links to other units",
    notes = "children represents a mapping from each child's unique identifier to the associated unitType; " +
    "vars simply passes through the representation of the unit received from the control API (and so is not defined here)",
    response = classOf[enterprise.examples.EnterpriseLinkedUnitForSwagger],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "Either an enterprise could not be found with the specified ERN and Period, or no related unit links are defined"),
    new ApiResponse(code = 500, message = "The attempt to retrieve the enterprise could not complete due to an unrecoverable error"),
    new ApiResponse(code = 504, message = "A response was not received from a data server within the required time interval")
  ))
  def retrieveEnterpriseLinkedUnit(
    @ApiParam(value = "Period (unit load date) - in YYYYMM format", example = "201803", required = true) periodStr: String,
    @ApiParam(value = "Enterprise Reference Number (ERN) - a ten digit number", example = "1000000012", required = true) ernStr: String
  ): Action[AnyContent] =
    retrieveLinkedUnit(periodStr, ernStr)
}

/*
 * DO NOT USE.
 * These are only defined to help Swagger generate a suitable example of an Enterprise response.
 *
 * Note that we are using a nested package to isolate these classes.  Attempts to use nested classes caused issues,
 * because they must be specified to Swagger in Java format (parentname$childname) but the $ symbol generates warnings
 * in Scala about a possible interpolation error, and Scala has no support for suppressing individual warnings.
 */
package enterprise.examples {
  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class EnterpriseChildrenForSwagger(
    @ApiModelProperty(value = "the unique identifier of the child followed by the child's type", dataType = "string", example = "LEU", required = true) `10205415`: UnitType,
    @ApiModelProperty(value = "the unique identifier of the child followed by the child's type", dataType = "string", example = "LOU", required = true) `900000011`: UnitType
  )

  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class EnterpriseLinkedUnitForSwagger(
    @ApiModelProperty(value = "the Enterprise Reference Number (ERN)", dataType = "string", example = "1234567890", required = true) id: UnitId,
    @ApiModelProperty(value = "the type of unit", dataType = "string", example = "ENT", required = true) unitType: UnitType,
    @ApiModelProperty(value = "Period (unit load date) - in YYYYMM format", dataType = "string", example = "201803", required = true) period: Period,
    @ApiModelProperty(value = "the identifiers of child units along with the associated unit type", dataType = "controllers.v1.enterprise.examples.EnterpriseChildrenForSwagger", required = false) children: Option[Map[UnitId, UnitType]],
    @ApiModelProperty(value = "the representation of the unit itself", dataType = "object", required = true) vars: JsObject
  )
}