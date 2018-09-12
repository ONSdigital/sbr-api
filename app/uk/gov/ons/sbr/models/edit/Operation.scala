package uk.gov.ons.sbr.models.edit

import play.api.libs.json._

case class Operation(op: OperationType, path: String, value: JsValue)

object Operation {
  implicit val opWrites = new Writes[Operation] {
    override def writes(o: Operation): JsValue = {
      import o._
      JsObject(Seq(
        "op" -> Json.toJson(op),
        "path" -> JsString(path),
        "value" -> value
      ))
    }
  }
}
