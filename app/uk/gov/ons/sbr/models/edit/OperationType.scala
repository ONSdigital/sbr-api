package uk.gov.ons.sbr.models.edit

import play.api.libs.json._

sealed trait OperationType

object OperationTypes {
  case object Test extends OperationType
  case object Replace extends OperationType
  case object Add extends OperationType
  case object Remove extends OperationType
}

object OperationType {
  implicit val writes = new Writes[OperationType] {
    override def writes(op: OperationType): JsValue = JsString(op.toString.toLowerCase)
  }
}