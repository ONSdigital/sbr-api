package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}

@Singleton
class BadRequestController @Inject() (components: ControllerComponents) extends AbstractSbrController(components) {
  def badRequest(arg1: String, arg2: String): Action[AnyContent] = Action {
    BadRequest
  }
}
