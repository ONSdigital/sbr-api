package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations.{ Api, _ }
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.JsObject
import play.api.mvc.{ Action, AnyContent, Result }
import uk.gov.ons.sbr.models.{ CompanyRefNumber, Period, UnitId, UnitType }
import unitref.CompaniesHouseUnitRef

@Api("Companies House")
@Singleton
class CompaniesHouseController @Inject() (
    retrieveLinkedUnitAction: LinkedUnitRequestActionBuilderMaker[CompanyRefNumber],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends LinkedUnitController[CompanyRefNumber](CompaniesHouseUnitRef, retrieveLinkedUnitAction, handleLinkedUnitRetrievalResult) {
  @ApiOperation(
    value = "Json representation of the Companies House unit along with its links to other units",
    notes = "parents represent a mapping from a parent unitType to the associated unit identifier; " +
    "vars simply passes through the variables defined by the admin data (and so is not defined here)",
    response = classOf[ch.examples.CompaniesHouseLinkedUnitForSwagger],
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "Either a Companies House unit could not be found with the specified company reference number and Period, or no related unit links are defined"),
    new ApiResponse(code = 500, message = "The attempt to retrieve the Companies House unit could not complete due to an unrecoverable error"),
    new ApiResponse(code = 504, message = "A response was not received from a data server within the required time interval")
  ))
  def retrieveCompaniesHouseLinkedUnit(
    @ApiParam(value = "Period (unit load date) - in YYYYMM format", example = "201803", required = true) periodStr: String,
    @ApiParam(value = "Company reference number - an eight digit number", example = "12345678", required = true) crnStr: String
  ): Action[AnyContent] =
    retrieveLinkedUnit(periodStr, crnStr)
}

/*
 * DO NOT USE.
 * These are only defined to help Swagger generate a suitable example of a Companies House response.
 *
 * Note that we are using a nested package to isolate these classes.  Attempts to use nested classes caused issues,
 * because they must be specified to Swagger in Java format (parentname$childname) but the $ symbol generates warnings
 * in Scala about a possible interpolation error, and Scala has no support for suppressing individual warnings.
 */
package ch.examples {
  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class CompaniesHouseParentForSwagger(
    @ApiModelProperty(value = "the unit type of the parent followed by the parent's unique identifier", dataType = "string", example = "1020541592", required = true) ENT: String,
    @ApiModelProperty(value = "the unit type of the parent followed by the parent's unique identifier", dataType = "string", example = "1234567890123456", required = true) LEU: String
  )

  @deprecated(message = "This is just for Swagger example purposes - do not use in real code", since = "")
  case class CompaniesHouseLinkedUnitForSwagger(
    @ApiModelProperty(value = "the Companies House company reference number", dataType = "string", example = "12345678", required = true) id: UnitId,
    @ApiModelProperty(value = "the type of unit", dataType = "string", example = "CH", required = true) unitType: UnitType,
    @ApiModelProperty(value = "Period (unit load date) - in YYYYMM format", dataType = "string", example = "201803", required = true) period: Period,
    @ApiModelProperty(value = "the types of parent units along with the associated unit identifiers", dataType = "controllers.v1.ch.examples.CompaniesHouseParentForSwagger", required = false) parents: Option[Map[UnitType, UnitId]],
    @ApiModelProperty(value = "the representation of the unit itself", dataType = "object", required = true) vars: JsObject
  )
}