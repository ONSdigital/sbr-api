package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.JsObject
import play.api.mvc.{ Action, AnyContent, Result }
import uk.gov.ons.sbr.models.{ PayeRef, Period, UnitId, UnitType }
import unitref.PayeUnitRef

@Api("PAYE")
@Singleton
class PayeController @Inject() (
    retrieveLinkedUnitAction: LinkedUnitRequestActionBuilderMaker[PayeRef],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends LinkedUnitController[PayeRef](PayeUnitRef, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {
  @ApiOperation(
    value = "Json representation of the PAYE unit along with its links to other units",
    notes = "parents represent a mapping from a parent unitType to the associated unit identifier; " +
    "vars simply passes through the variables defined by the admin data (and so is not defined here)",
    response = classOf[paye.examples.PayeLinkedUnitForSwagger],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "Either a PAYE unit could not be found with the specified PAYE reference and Period, or no related unit links are defined"),
    new ApiResponse(code = 500, message = "The attempt to retrieve the PAYE unit could not complete due to an unrecoverable error"),
    new ApiResponse(code = 504, message = "A response was not received from a data server within the required time interval")
  ))
  def retrievePayeLinkedUnit(
    @ApiParam(value = "Period (unit load date) - in YYYYMM format", example = "201803", required = true) periodStr: String,
    @ApiParam(value = "PAYE reference - an alphanumeric reference between 4 & 12 characters in length", example = "915H7Z72878", required = true) payerefStr: String
  ): Action[AnyContent] =
    retrieveLinkedUnit(periodStr, payerefStr)
}

/*
 * DO NOT USE.
 * These are only defined to help Swagger generate a suitable example of a PAYE response.
 *
 * Note that we are using a nested package to isolate these classes.  Attempts to use nested classes caused issues,
 * because they must be specified to Swagger in Java format (parentname$childname) but the $ symbol generates warnings
 * in Scala about a possible interpolation error, and Scala has no support for suppressing individual warnings.
 */
package paye.examples {
  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class PayeParentForSwagger(@ApiModelProperty(value = "the unit type of the parent followed by the parent's unique identifier", dataType = "string", example = "1234567890123456", required = true) LEU: String)

  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class PayeLinkedUnitForSwagger(
    @ApiModelProperty(value = "the PAYE reference", dataType = "string", example = "915H7Z72878", required = true) id: UnitId,
    @ApiModelProperty(value = "the type of unit", dataType = "string", example = "PAYE", required = true) unitType: UnitType,
    @ApiModelProperty(value = "Period (unit load date) - in YYYYMM format", dataType = "string", example = "201803", required = true) period: Period,
    @ApiModelProperty(value = "the types of parent units along with the associated unit identifiers", dataType = "controllers.v1.paye.examples.PayeParentForSwagger", required = false) parents: Option[Map[UnitType, UnitId]],
    @ApiModelProperty(value = "the representation of the unit itself", dataType = "object", required = true) vars: JsObject
  )
}