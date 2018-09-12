package controllers.v1

import javax.inject.{ Inject, Singleton }

import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations.{ Api, ApiOperation, ApiResponse, ApiResponses }
import parsers.JsonUnitLinkEditBodyParser
import play.api.mvc.{ Action, Controller, Result }
import repository.sbrctrl.RestAdminDataUnitLinksEditRepository
import repository.EditSuccess
import services.PatchCreationService
import uk.gov.ons.sbr.models.{ Period, VatRef }
import uk.gov.ons.sbr.models.edit.Patch

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Api("AdminDataEdit")
@Singleton
class AdminDataParentLinkEditController @Inject() (
    patchService: PatchCreationService,
    repository: RestAdminDataUnitLinksEditRepository
) extends Controller with LazyLogging {

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
    new ApiResponse(code = 422, message = "The request cannot be processed, e.g. specified LEU does not exist"),
    new ApiResponse(code = 409, message = "An edit conflict has occurred"),
    new ApiResponse(code = 500, message = "The attempt to edit the VAT parent unit link could not complete due to an unrecoverable error")
  ))
  def editVatParentLink(periodStr: String, vatrefStr: String) = Action.async(JsonUnitLinkEditBodyParser) { request =>
    patchService.createPatch(request.body).fold(
      resultOnFailure, patch => resultOnPatchConversionSuccess(patch, Period.fromString(periodStr), VatRef(vatrefStr))
    )
  }

  private def resultOnFailure(errorMessage: String): Future[Result] = Future(InternalServerError(errorMessage))

  private def resultOnPatchConversionSuccess(patch: Patch, period: Period, vatref: VatRef): Future[Result] = {
    repository.patchVatParentUnitLink(patch, period, vatref) map {
      case EditSuccess => Created
      case _ => InternalServerError
    }
  }
}