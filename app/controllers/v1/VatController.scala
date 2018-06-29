package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.JsObject
import play.api.mvc.{ Action, AnyContent, Result }
import uk.gov.ons.sbr.models.{ Period, UnitId, UnitType, _ }
import unitref.UnitRef

@Api("VAT")
@Singleton
class VatController @Inject() (
    unitRefType: UnitRef[VatRef],
    retrieveLinkedUnitAction: LinkedUnitRequestActionBuilderMaker[VatRef],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends LinkedUnitController[VatRef](unitRefType, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {
  @ApiOperation(
    value = "Json representation of the VAT unit along with its links to other units",
    notes = "parents represent a mapping from a parent unitType to the associated unit identifier; " +
    "vars simply passes through the variables defined by the admin data (and so is not defined here)",
    response = classOf[vat.examples.VatLinkedUnitForSwagger],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "Either a VAT unit could not be found with the specified VAT reference and Period, or no related unit links are defined"),
    new ApiResponse(code = 500, message = "The attempt to retrieve the VAT unit could not complete due to an unrecoverable error"),
    new ApiResponse(code = 504, message = "A response was not received from a data server within the required time interval")
  ))
  def retrieveVatLinkedUnit(
    @ApiParam(value = "Period (unit load date) - in YYYYMM format", example = "201803", required = true) periodStr: String,
    @ApiParam(value = "VAT reference - a twelve digit number", example = "123456789012", required = true) vatrefStr: String
  ): Action[AnyContent] =
    retrieveLinkedUnit(periodStr, vatrefStr)
}

/*
 * DO NOT USE.
 * These are only defined to help Swagger generate a suitable example of a VAT response.
 *
 * Note that we are using a nested package to isolate these classes.  Attempts to use nested classes caused issues,
 * because they must be specified to Swagger in Java format (parentname$childname) but the $ symbol generates warnings
 * in Scala about a possible interpolation error, and Scala has no support for suppressing individual warnings.
 */
package vat.examples {
  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class VatParentForSwagger(@ApiModelProperty(value = "the unit type of the parent followed by the parent's unique identifier", dataType = "string", example = "1020541592", required = true) ENT: String)

  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class VatLinkedUnitForSwagger(
    @ApiModelProperty(value = "the VAT reference", dataType = "string", example = "123456789012", required = true) id: UnitId,
    @ApiModelProperty(value = "the type of unit", dataType = "string", example = "VAT", required = true) unitType: UnitType,
    @ApiModelProperty(value = "Period (unit load date) - in YYYYMM format", dataType = "string", example = "201803", required = true) period: Period,
    @ApiModelProperty(value = "the types of parent units along with the associated unit identifiers", dataType = "controllers.v1.vat.examples.VatParentForSwagger", required = false) parents: Option[Map[UnitType, UnitId]],
    @ApiModelProperty(value = "the representation of the unit itself", dataType = "object", required = true) vars: JsObject
  )
}