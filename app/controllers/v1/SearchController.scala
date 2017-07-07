package controllers.v1

import play.api.mvc.{ Action, AnyContent, Controller }
import utils.Utilities._
import play.api.libs.json._

/**
 * Created by haqa on 04/07/2017.
 */
class SearchController extends Controller with Source {

  def searchById(id: Option[String], origin: Option[String], switch: Boolean = false): Action[AnyContent] = {
    Action { implicit request =>
      val res = id match {
        case Some(id) if id.length > 0 => findRecord(id, "conf/sample/data.csv") match {
          case Nil => NotFound(errAsJson(404, "invalid parameter", s"Could not find value ${id}"))
          case x => Ok(Json.parse(s"""[${MatchObj.toMap(x)}]"""))
        }
        case _ => BadRequest(errAsJson(400, "missing parameter", "No query string found"))
      }
      res
    }

  }
}
