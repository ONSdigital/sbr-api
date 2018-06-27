package actions

import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.{ ErrorMessage, LinkedUnitService }
import uk.gov.ons.sbr.models.{ LinkedUnit, Period }

import scala.concurrent.Future

class LinkedUnitRequest[A](val linkedUnitResult: Either[ErrorMessage, Option[LinkedUnit]], originalRequest: Request[A]) extends WrappedRequest[A](originalRequest)

object RetrieveLinkedUnitAction {
  type LinkedUnitRequestActionBuilderMaker[T] = (Period, T) => ActionBuilder[LinkedUnitRequest]
}

class RetrieveLinkedUnitAction[T](linkedUnitService: LinkedUnitService[T]) extends LinkedUnitRequestActionBuilderMaker[T] with LazyLogging {
  def apply(period: Period, unitRef: T): ActionBuilder[LinkedUnitRequest] =
    new ActionBuilder[LinkedUnitRequest] {
      override def invokeBlock[A](request: Request[A], block: LinkedUnitRequest[A] => Future[Result]): Future[Result] =
        linkedUnitService.retrieve(period, unitRef).flatMap { errorOrOptLinkedUnit =>
          errorOrOptLinkedUnit.left.foreach(errorMessage => logger.error(errorMessage))
          block(new LinkedUnitRequest[A](errorOrOptLinkedUnit, request))
        }
    }
}
