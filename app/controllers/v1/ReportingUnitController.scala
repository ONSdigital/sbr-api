package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitTracedRequestActionFunctionMaker
import actions.TracedRequest
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.JsObject
import play.api.mvc.{ Action, ActionBuilder, AnyContent, Result }
import uk.gov.ons.sbr.models.{ Period, Rurn, UnitId, UnitType }
import unitref.UnitRef

@Api("Reporting Unit")
@Singleton
class ReportingUnitController @Inject() (
    unitRefType: UnitRef[Rurn],
    tracingAction: ActionBuilder[TracedRequest, AnyContent],
    retrieveLinkedUnitAction: LinkedUnitTracedRequestActionFunctionMaker[Rurn],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends LinkedUnitController[Rurn](unitRefType, tracingAction, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {
  @ApiOperation(
    value = "Json representation of the reporting unit along with its links to other units",
    notes = "parents represent a mapping from the parent unitType to its associated unique identifier; children represent a mapping from a unique identifier to the associated child unitType; " +
    "vars simply passes through the representation of the unit received from the control API (and so is not defined here)",
    response = classOf[reportingunit.examples.ReportingLinkedUnitForSwagger],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "Either a reporting unit could not be found with the specified RURN and Period, or no related unit links are defined"),
    new ApiResponse(code = 500, message = "The attempt to retrieve the reporting unit could not complete due to an unrecoverable error"),
    new ApiResponse(code = 504, message = "A response was not received from a data server within the required time interval")
  ))
  def retrieveReportingLinkedUnit(
    @ApiParam(value = "Period (unit load date) - in YYYYMM format", example = "201803", required = true) periodStr: String,
    @ApiParam(value = "Reporting Unit Reference Number (RURN) - an eleven digit number", example = "12345678901", required = true) rurnStr: String
  ): Action[AnyContent] =
    retrieveLinkedUnit(periodStr, rurnStr)
}

/*
 * DO NOT USE.
 * These are only defined to help Swagger generate a suitable example of a Reporting Unit response.
 *
 * Note that we are using a nested package to isolate these classes.  Attempts to use nested classes caused issues,
 * because they must be specified to Swagger in Java format (parentname$childname) but the $ symbol generates warnings
 * in Scala about a possible interpolation error, and Scala has no support for suppressing individual warnings.
 */
package reportingunit.examples {
  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class ReportingUnitParentsForSwagger(
    @ApiModelProperty(value = "the unitType of the parent followed by its unique unit identifier", dataType = "string", example = "1234567890", required = true) ENT: UnitId
  )

  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class ReportingUnitChildrenForSwagger(
    @ApiModelProperty(value = "the unique unit identifier of the child followed by its associated unitType", dataType = "string", example = "LOU", required = true) `987654321`: UnitType
  )

  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class ReportingLinkedUnitForSwagger(
    @ApiModelProperty(value = "the Reporting Unit Reference Number (RURN)", dataType = "string", example = "12345678901", required = true) id: UnitId,
    @ApiModelProperty(value = "the type of unit", dataType = "string", example = "REU", required = true) unitType: UnitType,
    @ApiModelProperty(value = "Period (unit load date) - in YYYYMM format", dataType = "string", example = "201803", required = true) period: Period,
    @ApiModelProperty(value = "the unitType of parents along with the associated unit identifier", dataType = "controllers.v1.reportingunit.examples.ReportingUnitParentsForSwagger", required = false) parents: Option[Map[UnitType, UnitId]],
    @ApiModelProperty(value = "the unitId of children along with the associated unit type", dataType = "controllers.v1.reportingunit.examples.ReportingUnitChildrenForSwagger", required = false) children: Option[Map[UnitId, UnitType]],
    @ApiModelProperty(value = "the representation of the unit itself", dataType = "object", required = true) vars: JsObject
  )
}