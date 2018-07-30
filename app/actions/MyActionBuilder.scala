package actions

import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }

abstract class MyActionBuilder[+R[_]](ec: ExecutionContext) extends ActionBuilder[R] {
  self =>

  override protected def executionContext: ExecutionContext =
    ec

  override def andThen[Q[_]](other: ActionFunction[R, Q]): ActionBuilder[Q] = new ActionBuilder[Q] {
    override def invokeBlock[A](request: Request[A], block: Q[A] => Future[Result]): Future[Result] =
      self.invokeBlock[A](request, other.invokeBlock[A](_, block))

    override protected def executionContext: ExecutionContext =
      self.executionContext

    override protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] =
      self.composeParser(bodyParser)

    override protected def composeAction[A](action: Action[A]): Action[A] =
      self.composeAction(action)
  }
}
