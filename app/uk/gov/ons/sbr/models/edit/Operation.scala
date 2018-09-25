package uk.gov.ons.sbr.models.edit

import play.api.libs.json._

case class Operation(op: OperationType, path: Path, value: JsValue)

/**
 * We separate out path into the prefix and value as once the Operation has been formed, the id in
 * path is required for creating a child unit link.
 */
case class Path(prefix: String, value: String)

object Operation {
  implicit val opWrites = new Writes[Operation] {
    override def writes(o: Operation): JsValue = {
      import o._
      JsObject(Seq(
        "op" -> Json.toJson(op),
        "path" -> JsString(s"${path.prefix}${path.value}"),
        "value" -> value
      ))
    }
  }
}