package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations.{ Api, _ }
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.JsObject
import play.api.mvc.{ Action, AnyContent, Result }
import uk.gov.ons.sbr.models.{ Lurn, Period, UnitId, UnitType }
import unitref.UnitRef

@Api("Local Unit")
@Singleton
class LocalUnitController @Inject() (
    unitRefType: UnitRef[Lurn],
    retrieveLinkedUnitAction: LinkedUnitRequestActionBuilderMaker[Lurn],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends LinkedUnitController[Lurn](unitRefType, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {
  @ApiOperation(
    value = "Json representation of the local unit along with its links to other units",
    notes = "parents represent a mapping from the parent unitType to its associated unique identifier; " +
    "vars simply passes through the representation of the unit received from the control API (and so is not defined here)",
    response = classOf[localunit.examples.LocalLinkedUnitForSwagger],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "Either a local unit could not be found with the specified LURN and Period, or no related unit links are defined"),
    new ApiResponse(code = 500, message = "The attempt to retrieve the local unit could not complete due to an unrecoverable error"),
    new ApiResponse(code = 504, message = "A response was not received from a data server within the required time interval")
  ))
  def retrieveLocalLinkedUnit(
    @ApiParam(value = "Period (unit load date) - in YYYYMM format", example = "201803", required = true) periodStr: String,
    @ApiParam(value = "Local Unit Reference Number (LURN) - a nine digit number", example = "123456789", required = true) lurnStr: String
  ): Action[AnyContent] =
    retrieveLinkedUnit(periodStr, lurnStr)
}

/*
 * DO NOT USE.
 * These are only defined to help Swagger generate a suitable example of a Local Unit response.
 *
 * Note that we are using a nested package to isolate these classes.  Attempts to use nested classes caused issues,
 * because they must be specified to Swagger in Java format (parentname$childname) but the $ symbol generates warnings
 * in Scala about a possible interpolation error, and Scala has no support for suppressing individual warnings.
 */
package localunit.examples {
  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class LocalUnitParentsForSwagger(
    @ApiModelProperty(value = "the unitType of the parent followed by its unique unit identifier", dataType = "string", example = "1234567890", required = true) ENT: UnitId
  )

  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class LocalLinkedUnitForSwagger(
    @ApiModelProperty(value = "the Local Unit Reference Number (LURN)", dataType = "string", example = "123456789", required = true) id: UnitId,
    @ApiModelProperty(value = "the type of unit", dataType = "string", example = "LOU", required = true) unitType: UnitType,
    @ApiModelProperty(value = "Period (unit load date) - in YYYYMM format", dataType = "string", example = "201803", required = true) period: Period,
    @ApiModelProperty(value = "the unitType of parents along with the associated unit identifier", dataType = "controllers.v1.localunit.examples.LocalUnitParentsForSwagger", required = false) parents: Option[Map[UnitType, UnitId]],
    @ApiModelProperty(value = "the representation of the unit itself", dataType = "object", required = true) vars: JsObject
  )
}