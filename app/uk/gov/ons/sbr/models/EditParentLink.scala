package uk.gov.ons.sbr.models

import play.api.libs.json._

/**
 * We get the following JSON POSTed to us from the user interface:
 *
 *  {
 *    "parent": {
 *      "from": {
 *        "id": "UBRN-01",
 *        "type" : "LEU"
 *      },
 *      "to": {
 *        "id": "UBRN-02",
 *        "type" : "LEU"
 *      }
 *    }
 *    "audit": { "username": "abcd" }
 *  }
 *
 * Which will be parsed by a JsonBodyParser into the model below.
 */

case class EditParentLink(parent: Parent, audit: Map[String, String])
case class Parent(from: IdAndType, to: IdAndType)
case class IdAndType(id: UnitId, `type`: UnitType)

object EditParentLink {
  implicit val editFormat: Format[EditParentLink] = Json.format[EditParentLink]
}

object Parent {
  implicit val parentFormat: Format[Parent] = Json.format[Parent]
}

object IdAndType {
  implicit val unitIdFormat: Format[UnitId] = UnitId.JsonFormat
  implicit val unitTypeFormat: Format[UnitType] = UnitType.JsonFormat
  implicit val idAndTypeFormat: Format[IdAndType] = Json.format[IdAndType]
}