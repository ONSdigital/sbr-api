package controllers.v1

import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations.{ Api, ApiOperation, ApiResponse, ApiResponses }
import javax.inject.{ Inject, Singleton }
import parsers.JsonUnitLinkEditBodyParser
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, Controller, Result }
import services._
import uk.gov.ons.sbr.models._

@Api("AdminDataEdit")
@Singleton
class AdminDataParentLinkEditController @Inject() (editService: EditService) extends Controller with LazyLogging {

  @ApiOperation(
    value = "Submit JSON with edit details for editing a VAT Parent Unit Link from one value to another",
    notes = """Use the following template: {"parent": "from": {"id":"123456789", "type":"LEU"}, "to": {"id":"123456789", "type":"LEU"}}""",
    consumes = "application/json",
    code = 201,
    httpMethod = "POST"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "The specified VAT reference could not be found in the Unit Links table"),
    new ApiResponse(code = 409, message = "An edit conflict has occurred"),
    new ApiResponse(code = 422, message = "The request cannot be processed, e.g. specified LEU does not exist"),
    new ApiResponse(code = 500, message = "The attempt to edit the VAT parent unit link could not complete due to an unrecoverable error")
  ))
  def editVatParentLink(periodStr: String, vatrefStr: String) = Action.async(JsonUnitLinkEditBodyParser) { request =>
    editService.editVatAdminDataParentUnitLink(Period.fromString(periodStr), VatRef(vatrefStr), request.body)
      .map(editStatusToHttpStatus)
  }

  @ApiOperation(
    value = "Submit JSON with edit details for editing a PAYE Parent Unit Link from one value to another",
    notes = """Use the following template: {"parent": "from": {"id":"123456789", "type":"LEU"}, "to": {"id":"123456789", "type":"LEU"}}""",
    consumes = "application/json",
    code = 201,
    httpMethod = "POST"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "One or more arguments do not comply with the expected format"),
    new ApiResponse(code = 404, message = "The specified PAYE reference could not be found in the Unit Links table"),
    new ApiResponse(code = 409, message = "An edit conflict has occurred"),
    new ApiResponse(code = 422, message = "The request cannot be processed, e.g. specified LEU does not exist"),
    new ApiResponse(code = 500, message = "The attempt to edit the PAYE parent unit link could not complete due to an unrecoverable error")
  ))
  def editPayeParentLink(periodStr: String, payerefStr: String) = Action.async(JsonUnitLinkEditBodyParser) { request =>
    editService.editPayeAdminDataParentUnitLink(Period.fromString(periodStr), PayeRef(payerefStr), request.body)
      .map(editStatusToHttpStatus)
  }

  private def editStatusToHttpStatus(editStatus: EditParentLinkStatus): Result = editStatus match {
    case EditSuccess => Created
    case EditUnitNotFound => NotFound
    case EditConflict => Conflict
    case EditRejected => UnprocessableEntity
    case _ => InternalServerError
  }
}