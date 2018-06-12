package controllers.v1

import controllers.v1.api.vat.VatUnitApi
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, AnyContent, Controller, Result }
import services.VatService
import uk.gov.ons.sbr.models._

/*
 * Note that we are relying on regex patterns in the routes definitions to apply argument validation.
 * Only requests with valid arguments should be routed to the retrieve... actions.
 * All other requests should be routed to the badRequest action.
 */
@Api("VAT")
@Singleton
class VatController @Inject() (vatService: VatService, linkedUnitRetrievalHandler: LinkedUnitRetrievalHandler[Result]) extends Controller with VatUnitApi {
  override def retrieveVat(periodStr: String, vatrefStr: String): Action[AnyContent] = Action.async {
    vatService.retrieve(Period.fromString(periodStr), VatRef(vatrefStr)).map {
      linkedUnitRetrievalHandler.handleOutcome
    }
  }

  def badRequest(periodStr: String, vatrefStr: String): Action[AnyContent] = Action {
    BadRequest
  }
}
