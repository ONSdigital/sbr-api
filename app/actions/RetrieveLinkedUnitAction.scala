package actions

import actions.RetrieveLinkedUnitAction.LinkedUnitTracedRequestActionFunctionMaker
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import play.api.mvc._
import services.{ ErrorMessage, LinkedUnitService }
import uk.gov.ons.sbr.models.{ LinkedUnit, Period }

import scala.concurrent.Future

class LinkedUnitTracedRequest[A](val linkedUnitResult: Either[ErrorMessage, Option[LinkedUnit]], tracedRequest: TracedRequest[A]) extends WrappedRequest[A](tracedRequest)

object RetrieveLinkedUnitAction {
  type LinkedUnitTracedRequestActionFunctionMaker[T] = (Period, T) => ActionTransformer[TracedRequest, LinkedUnitTracedRequest]
}

class RetrieveLinkedUnitAction[T](linkedUnitService: LinkedUnitService[T], ec: ExecutionContext) extends LinkedUnitTracedRequestActionFunctionMaker[T] with LazyLogging {
  def apply(period: Period, unitRef: T): ActionTransformer[TracedRequest, LinkedUnitTracedRequest] =
    new ActionTransformer[TracedRequest, LinkedUnitTracedRequest] {
      override protected def transform[A](request: TracedRequest[A]): Future[LinkedUnitTracedRequest[A]] = {
        logger.info(s"Inside RetrieveLinkedUnitAction before service call in [${Thread.currentThread().getName}]")
        linkedUnitService.retrieve(period, unitRef, request.traceData).map { errorOrOptLinkedUnit =>
          errorOrOptLinkedUnit.left.foreach(errorMessage => logger.error(errorMessage))
          logger.info(s"Inside RetrieveLinkedUnitAction after service call in [${Thread.currentThread().getName}]")
          new LinkedUnitTracedRequest[A](errorOrOptLinkedUnit, request)
        }(ec)
      }

      /*
       * An ActionTransformer inherits the default Play executionContext by virtue of being an ActionFunction.
       * Use the injected context instead.
       */
      override protected def executionContext: ExecutionContext =
        ec
    }
}
