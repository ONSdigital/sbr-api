package controllers.v1

import java.lang.annotation.Annotation
//import javax.ws.rs.GET

import io.swagger.annotations._
import play.api.mvc.{ Action, AnyContent, Controller }
import utils.Utilities._
import play.api.libs.json._

/**
 * Created by haqa on 04/07/2017.
 */
@Api("Search")
class SearchController extends Controller with Source {

  //public api
  @ApiOperation(
    value = "Returns a json list that matches the given anonymous id",
    notes = "The matches can occur from any id field (hence anonymous) and multiple records can be matched",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, response = classOf[Matches], responseContainer = "JSONObject", message = "Success -> Record(s) found for id."),
    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Client Side Error -> Required parameter was not found."),
    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Client Side Error -> Id not found."),
    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Server Side Error -> Request could not be completed.")
  ))
  def searchById(id: Option[String], origin: Option[String]): Action[AnyContent] = {
    Action { implicit request =>
      val res = id match {
        case Some(id) if id.length > 0 => findRecord(id, "conf/sample/data.csv") match {
          case Nil => NotFound(errAsJson(404, "not found", s"Could not find value ${id}"))
          case x => Ok(Json.parse(s"""[${MatchObj.toString(x)}]"""))
        }
        case _ => BadRequest(errAsJson(400, "missing parameter", "No query string found"))
      }
      res
    }
  }

}

//    {
//      override def code(): Int = ???
//
//      override def message(): String = ???
//
//      override def reference(): String = ???
//
//      override def responseHeaders(): Array[ResponseHeader] = ???
//
//      override def response(): Class[_] = ???
//
//      override def responseContainer(): String = ???
//
//      override def annotationType(): Class[_ <: Annotation] = ???
//    }
