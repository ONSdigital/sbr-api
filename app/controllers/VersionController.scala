package controllers

import javax.inject.Singleton
import play.api.mvc.{ Action, Controller }

/**
 * ...............
 */
@Singleton
class VersionController extends Controller {
  def version = Action {
    Ok("some version information list!!")
  }
}