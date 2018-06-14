package controllers

import javax.inject.Singleton
import play.api.mvc.{ Action, AnyContent, Controller }

@Singleton
class BadRequestController extends Controller {
  def badRequest(arg1: String, arg2: String): Action[AnyContent] = Action {
    BadRequest
  }
}
