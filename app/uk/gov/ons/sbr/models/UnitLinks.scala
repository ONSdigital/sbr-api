package uk.gov.ons.sbr.models

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._
import uk.gov.ons.sbr.models.UnitType.Enterprise
import unitref.UnitRef
import utils.JsResultSupport

case class UnitLinks(
  id: UnitId,
  unitType: UnitType,
  period: Period,
  parents: Option[Map[UnitType, UnitId]],
  children: Option[Map[UnitId, UnitType]]
)

object UnitLinks extends LazyLogging {
  val reads: Reads[UnitLinks] = ReadsUnitLinks

  /*
   * The default deserializer only supports Maps with String keys (given the underlying representation as a Json object).
   * To work around this, we simply model the keys as strings in an "external form", deserialize to that form, and then
   * convert the result.
   */
  private object ReadsUnitLinks extends Reads[UnitLinks] {
    private case class ExternalForm(
      id: UnitId,
      unitType: UnitType,
      period: Period,
      parents: Option[Map[String, UnitId]],
      children: Option[Map[String, UnitType]]
    )

    private implicit val readsUnitId: Reads[UnitId] = UnitId.JsonFormat
    private implicit val readsUnitType: Reads[UnitType] = UnitType.JsonFormat
    private implicit val readsPeriod: Reads[Period] = Period.JsonFormat
    private implicit val readsExternalForm: Reads[ExternalForm] = Json.reads[ExternalForm]

    override def reads(json: JsValue): JsResult[UnitLinks] = {
      val result = json.validate[ExternalForm].flatMap(fromExternalForm)
      result.foreach { unitLinks =>
        if (unitLinks.parents.isEmpty && unitLinks.children.isEmpty) {
          logger.warn(s"Encountered UnitLinks definition with no parents and no children [$unitLinks]")
        }
      }
      result
    }

    private def fromExternalForm(ef: ExternalForm): JsResult[UnitLinks] = {
      val parentsResult = fromExternalParents(ef.parents)
      parentsResult.map { parents =>
        UnitLinks(
          ef.id,
          ef.unitType,
          ef.period,
          parents,
          ef.children.flatMap(fromExternalChildren)
        )
      }
    }

    private def fromExternalParents(parents: Option[Map[String, UnitId]]): JsResult[Option[Map[UnitType, UnitId]]] =
      parents.map(fromExternalParentsMap).fold[JsResult[Option[Map[UnitType, UnitId]]]](JsSuccess(None)) { result =>
        result.map(nonEmptyMap)
      }

    private def fromExternalParentsMap(parents: Map[String, UnitId]): JsResult[Map[UnitType, UnitId]] = {
      val parentResults = parents.toSeq.map {
        case (k, v) =>
          fromExternalUnitType(k).map(_ -> v)
      }
      JsResultSupport.sequence(parentResults).map(values => Map(values: _*))
    }

    private def fromExternalUnitType(acronym: String): JsResult[UnitType] =
      JsResultSupport.fromOption(UnitType.fromAcronym(acronym))

    private def fromExternalChildren(children: Map[String, UnitType]): Option[Map[UnitId, UnitType]] =
      nonEmptyMap(convertChildKeyToUnitId(children))

    private def convertChildKeyToUnitId(children: Map[String, UnitType]): Map[UnitId, UnitType] =
      children.map {
        case (k, v) =>
          UnitId(k) -> v
      }

    private def nonEmptyMap[K, V](map: Map[K, V]): Option[Map[K, V]] =
      if (map.isEmpty) None else Some(map)
  }

  def parentErnFrom(ernType: UnitRef[Ern])(unitLinks: UnitLinks): Option[Ern] =
    unitLinks.parents.flatMap(_.get(Enterprise)).map {
      ernType.fromUnitId
    }
}