package controllers.v1

import controllers.v1.api.enterprise.EnterpriseUnitApi
import handlers.LinkedUnitRetrievalHandler
import io.swagger.annotations._
import javax.inject.{ Inject, Singleton }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Action, AnyContent, Controller, Result }
import services.EnterpriseService
import uk.gov.ons.sbr.models.{ Ern, Period }

/*
 * Note that we are relying on regex patterns in the routes definitions to apply argument validation.
 * Only requests with valid arguments should be routed to the retrieve... actions.
 * All other requests should be routed to the badRequest action.
 */
@Api("Enterprise")
@Singleton
class EnterpriseController @Inject() (enterpriseService: EnterpriseService, linkedUnitRetrievalHandler: LinkedUnitRetrievalHandler[Result]) extends Controller with EnterpriseUnitApi {
  override def retrieveEnterprise(periodStr: String, ernStr: String): Action[AnyContent] = Action.async {
    enterpriseService.retrieve(Period.fromString(periodStr), Ern(ernStr)).map {
      linkedUnitRetrievalHandler.handleOutcome
    }
  }

  def badRequest(periodStr: String, ernStr: String): Action[AnyContent] = Action {
    BadRequest
  }
}
