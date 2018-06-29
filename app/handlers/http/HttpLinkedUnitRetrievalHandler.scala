package handlers.http

import handlers.LinkedUnitRetrievalHandler
import javax.inject.Inject
import play.api.libs.json.{ Json, Writes }
import play.api.mvc.Result
import play.api.mvc.Results.{ GatewayTimeout, InternalServerError, NotFound, Ok }
import services.ErrorMessage
import uk.gov.ons.sbr.models.LinkedUnit

class HttpLinkedUnitRetrievalHandler @Inject() (writesLinkedUnit: Writes[LinkedUnit]) extends LinkedUnitRetrievalHandler[Result] {
  override def apply(outcome: Either[ErrorMessage, Option[LinkedUnit]]): Result =
    outcome.fold(resultOnFailure, resultOnSuccess)

  private def resultOnFailure(errorMessage: ErrorMessage): Result =
    errorMessage match {
      case _ if errorMessage.startsWith("Timeout") => GatewayTimeout
      case _ => InternalServerError
    }

  private def resultOnSuccess(optLinkedUnit: Option[LinkedUnit]): Result =
    optLinkedUnit.fold[Result](NotFound) { linkedUnit =>
      Ok(Json.toJson(linkedUnit)(writesLinkedUnit))
    }
}
