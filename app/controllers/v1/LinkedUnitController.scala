package controllers.v1

import actions.RetrieveLinkedUnitAction.LinkedUnitTracedRequestActionFunctionMaker
import actions.TracedRequest
import handlers.LinkedUnitRetrievalHandler
import play.api.mvc._
import play.mvc.Controller
import uk.gov.ons.sbr.models.{ Period, UnitId }
import unitref.UnitRef

/*
 * Note that we are relying on regex patterns in the routes definitions to apply argument validation.
 * Only requests with valid arguments should be routed to the retrieveLinkedUnit action.
 */
private[v1] class LinkedUnitController[T](
    unitRefType: UnitRef[T],
    withTracingAction: ActionBuilder[TracedRequest, AnyContent],
    retrieveLinkedUnitAction: LinkedUnitTracedRequestActionFunctionMaker[T],
    handleLinkedUnitRetrievalResult: LinkedUnitRetrievalHandler[Result]
) extends Controller {
  protected def retrieveLinkedUnit(periodStr: String, unitRefStr: String): Action[AnyContent] = {
    val period = Period.fromString(periodStr)
    val unitRef = unitRefType.fromUnitId(UnitId(unitRefStr))

    withTracingAction.andThen(retrieveLinkedUnitAction(period, unitRef)) { request =>
      handleLinkedUnitRetrievalResult(request.linkedUnitResult)
    }
  }
}
