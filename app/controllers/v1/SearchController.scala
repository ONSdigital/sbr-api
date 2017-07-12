package controllers.v1

import io.swagger.annotations._
import play.api.mvc.{ Action, AnyContent }
import utils.Utilities._
import com.outworkers.util.play._

/**
 * Created by haqa on 04/07/2017.
 */
@Api("Search")
class SearchController extends ControllerUtils {

  //public api
  @ApiOperation(
    value = "Json list of id matches",
    notes = "The matches can occur from any id field and multiple records can be matched",
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
  def searchById(
    @ApiParam(value = "An identifier of any type", example = "825039145000", required = true) id: Option[String],
    @ApiParam(value = "term to categories the id source", required = true) origin: Option[String]
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      val res = id match {
        case Some(id) if id.length > 0 => findRecord(id, "conf/sample/data.csv") match {
          case Nil => NotFound(errAsJson(404, "not found", s"Could not find value ${id}")).future
          case x => Ok(s"""[${MatchObj.toString(x)}]""").future
        }
        case _ => BadRequest(errAsJson(400, "missing parameter", "No query string found")).future
      }
      res
    }
  }

}