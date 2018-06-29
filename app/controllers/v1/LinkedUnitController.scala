package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import handlers.LinkedUnitRetrievalHandler
import play.api.mvc.{ Action, AnyContent, Result }
import play.mvc.Controller
import uk.gov.ons.sbr.models.{ Period, UnitId }
import unitref.UnitRef

/*
 * Note that we are relying on regex patterns in the routes definitions to apply argument validation.
 * Only requests with valid arguments should be routed to the retrieveLinkedUnit action.
 */
private[v1] class LinkedUnitController[T](
    unitRefType: UnitRef[T],
    retrieveLinkedUnitAction: LinkedUnitRequestActionBuilderMaker[T],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends Controller {
  protected def retrieveLinkedUnit(periodStr: String, unitRefStr: String): Action[AnyContent] =
    retrieveLinkedUnitAction(Period.fromString(periodStr), unitRefType.fromUnitId(UnitId(unitRefStr))) { request =>
      handleLinkedUnitRetrievalResult(request.linkedUnitResult)
    }
}
