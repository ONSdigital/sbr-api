package controllers.v1

import java.time.format.DateTimeParseException

import com.netaporter.uri.Uri
import com.typesafe.scalalogging.StrictLogging
import config.Properties
import javax.naming.ServiceUnavailableException
import org.slf4j.{ Logger, LoggerFactory }
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.mvc.{ Controller, Result }
import services.RequestGenerator
import uk.gov.ons.sbr.models._
import utils.FutureResponse.futureSuccess
import utils.UriBuilder.createUri
import utils.Utilities.{ errAsJson, orElseNull }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, TimeoutException }


// @todo - fix typedef
trait ControllerUtils extends Controller with StrictLogging with Properties with I18nSupport {

  protected val PLACEHOLDER_PERIOD = "*date"
  // number of units displayable
  private val CAPPED_DISPLAY_NUMBER = 1
  protected val FIXED_YEARMONTH_SIZE = 6

  protected[this] val LOGGER: Logger = LoggerFactory.getLogger(getClass.getName)

  protected type UnitLinksListType = Seq[JsValue]
  protected type StatisticalUnitLinkType = JsValue

  private def toJson(record: (JsValue, JsValue), `type`: String, periodFromRowKey: String): JsValue = {
    Json.toJson(JsObject(record match {
      case (link, unit) =>

        // @ TODO PATCH - fix and remove patch
        // BI does not have period, so use an empty string
        val period = if (`type` == ENT.toString) {
          (link \ "period").getOrNull
        } else if (`type` == LOU.toString) {
          Json.toJson(periodFromRowKey)
        } else {
          (unit.as[JsValue] \ "period").getOrNull
        }

        // @ TODO PATCH - fix and remove patch when BI and ENTERPRISE apis are fixed
        // For BI, there is no "vars", just use the whole record
        //        val vars = if (`type` == ENT.toString || `type` == LEU.toString) {
        val vars = if (`type` == ENT.toString) {
          (unit \ "vars").getOrElse(unit)
        } else if (`type` == LOU.toString) {
          unit
        } else {
          (unit.as[JsValue] \ "variables").getOrNull
        }

        val unitType = DataSourceTypesUtil.fromString(`type`).getOrElse("").toString

        // Only return childrenJson with an Enterprise
        (unitType match {
          case "ENT" =>
            Json.obj(
              "id" -> (link \ "id").getOrNull,
              "parents" -> (link \ "parents").getOrNull,
              "children" -> (link \ "children").getOrNull,
              "childrenJson" -> (unit \ "childrenJson").getOrNull,
              "unitType" -> unitType,
              "period" -> period,
              "vars" -> vars
            )
          case _ =>
            Json.obj(
              "id" -> (link \ "id").getOrNull,
              "parents" -> (link \ "parents").getOrNull,
              "children" -> (link \ "children").getOrNull,
              "unitType" -> unitType,
              "period" -> period,
              "vars" -> vars
            )
        }).fields.filterNot { case (_, v) => v == JsNull }
    }))
  }

  private def responseException: PartialFunction[Throwable, Result] = {
    case ex: DateTimeParseException =>
      BadRequest(Messages("controller.datetime.failed.parse", ex.toString))
    case ex: RuntimeException =>
      InternalServerError(errAsJson(ex.toString, ex.getCause.toString))
    case ex: ServiceUnavailableException =>
      ServiceUnavailable(errAsJson(ex.toString, ex.getCause.toString))
    case ex: TimeoutException =>
      RequestTimeout(Messages("controller.timeout.request", s"$ex", s"${ex.getCause}"))
    case ex => InternalServerError(errAsJson(ex.toString, ex.getCause.toString))
  }

  protected def louSearch(baseUrl: Uri, id: String, period: String)(implicit ws: RequestGenerator): Future[Result] = id.trim match {
    case validId if (id.length >= MINIMUM_KEY_LENGTH) => {
      LOGGER.info(s"Sending request to ${baseUrl.toString} to retrieve Unit Links")
      ws.singleGETRequest(baseUrl.toString) map {
        case response if response.status == OK => {
          val unitResp = response.json.as[StatisticalUnitLinkType]
          val period = (unitResp \ "period").as[String]
          val path = createLouUri(SBR_CONTROL_API_URL, id, unitResp)
          LOGGER.info(s"Sending request to $path to get records of all variables of unit.")
          ws.singleGETRequestWithTimeout(path.toString, Duration.Inf) match {
            case response if response.status == OK => {
              val json = (Seq(unitResp) zip List(response.json)).map(x => toJson(x, LOU.toString, period)).head
              Ok(json).as(JSON)
            }
            case response if response.status == NOT_FOUND => {
              LOGGER.error(s"Found unit links for record with id [$id], but the corresponding record returns 404")
              NotFound(response.body).as(JSON)
            }
            case _ => InternalServerError(Messages("controller.internal.server.error"))
          }
        }
        case response if response.status == NOT_FOUND => NotFound(response.body).as(JSON)
        case _ => InternalServerError(Messages("controller.internal.server.error"))
      } recover responseException
    }
    case _ => BadRequest(Messages("controller.invalid.id", id, MINIMUM_KEY_LENGTH)).future
  }

  // @ TODO - CHECK error control
  protected def search[T](key: String, baseUrl: Uri, sourceType: DataSourceTypes = ENT, periodParam: Option[String] = None, history: Option[Int] = None)
                         (implicit fjs: Reads[T], ws: RequestGenerator): Future[Result] =
    key match {
      case k if k.length >= MINIMUM_KEY_LENGTH =>
        LOGGER.debug(s"Sending request to ${baseUrl.toString} to retrieve Unit Links")
        ws.singleGETRequest(baseUrl.toString) flatMap {
          case response if response.status == OK => {
            LOGGER.debug(s"Result for unit is: ${response.body}")
            // @ TODO - add to success or failure to JSON ??
            val unitResp = response.json.as[T]
            unitResp match {
              // UnitLinksListType is erased - the type of the content is unchecked
              case u: Seq[_] =>
                // if one UnitLinks found -> get unit
                if (u.length == CAPPED_DISPLAY_NUMBER) {
                  u.head match {
                    case j: JsValue =>
                      val id = (j \ "id").as[String]
                      LOGGER.debug(s"Found a single response with $id")
                      val unitType = (j \ "unitType").as[String]
                      val period = (j \ "period").as[String]
                      val mapOfRecordKeys = Map((unitType -> id)
                      parsedRequest(mapOfRecordKeys, j, periodParam, history).map { respRecords =>
                        val json: Seq[JsValue] = (Seq(j) zip respRecords).map(x => toJson(x, unitType, period))
                        Ok(Json.toJson(json)).as(JSON)
                      }
                    case _ => throw new AssertionError("Seq does not contain a JsValue")
                  }
                } else {
                  LOGGER.debug(s"Found multiple records matching given id, $key. Returning multiple as list.")
                  // return UnitLinks if multiple
                  PartialContent(unitResp.toString).as(JSON).future
                }
              case s: StatisticalUnitLinkType =>
                val mapOfRecordKeys = if (sourceType.toString == LOU) {
                  Map(sourceType.toString -> (s.head \ "id").as[String])
                } else {
                  Map(sourceType.toString -> (s \ "id").as[String])
                }
                val period = (s \ "period").as[String]
                parsedRequest(mapOfRecordKeys, s, periodParam).map { respRecords =>
                  val json = (Seq(s) zip respRecords).map(x => toJson(x, sourceType.toString, period)).head
                  Ok(json).as(JSON)
                }
            }
          }
          case response if response.status == NOT_FOUND =>
            NotFound(response.body).as(JSON).future
        } recover responseException
      case _ =>
        BadRequest(Messages("controller.invalid.id", key, MINIMUM_KEY_LENGTH)).future
    }

  private def parsedRequest(searchList: Map[String, String], unitLinksData: JsValue, withPeriod: Option[String], limit: Option[Int] = None)
                           (implicit ws: RequestGenerator): Future[Seq[JsValue]] = {
    val futures = searchList.flatMap {
      case (group, id) => lookupUnit(group, id, unitLinksData, withPeriod, limit)
    }.map { futResponse =>
      futResponse.map { resp =>
        LOGGER.debug(s"Result for record is: ${resp.body}")
        // @ TODO - add to success or failure to JSON ??
        resp.json
      }
    }.toSeq

    Future.sequence(futures)
  }

  private def lookupUnit(group: String, id: String, unitLinksData: JsValue, withPeriod: Option[String], limit: Option[Int])
                        (implicit ws: RequestGenerator): Option[Future[WSResponse]] = {
    val unitOpt = DataSourceTypesUtil.fromString(group)
    if (unitOpt.isEmpty) logger.warn(s"Unrecognised group value [$group].")
    unitOpt.map { unit =>
      val path = apiUrlFor(unit)
      val newPath = unit match {
        case LOU => createLouUri(path, id, unitLinksData)
        case _ => createUri(path, id, withPeriod, group = unit.toString, history = limit)
      }
      LOGGER.info(s"Sending request to $newPath to get records of all variables of unit.")
      // @TODO - Duration.Inf -> place cap
      ws.singleGETRequestWithTimeout(newPath.toString, Duration.Inf)
    }
  }

  private def apiUrlFor(unit: DataSourceTypes): String =
    unit match {
      case LEU => LEGAL_UNIT_DATA_API_URL
      case CRN => CH_ADMIN_DATA_API_URL
      case VAT => VAT_ADMIN_DATA_API_URL
      case PAYE => PAYE_ADMIN_DATA_API_URL
      case ENT => SBR_CONTROL_API_URL
      case LOU => SBR_CONTROL_API_URL
    }
}
