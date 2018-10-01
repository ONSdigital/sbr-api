package uk.gov.ons.sbr.models.edit

import play.api.libs.json._

sealed trait Operation {
  val path: Path
  val operationType: OperationType
}
sealed trait OperationWithValue extends Operation {
  val value: JsValue
}
case class AddOperation(path: Path, value: JsValue) extends OperationWithValue {
  val operationType = OperationTypes.Add
}
case class ReplaceOperation(path: Path, value: JsValue) extends OperationWithValue {
  val operationType = OperationTypes.Replace
}
case class TestOperation(path: Path, value: JsValue) extends OperationWithValue {
  val operationType = OperationTypes.Test
}
case class RemoveOperation(path: Path) extends Operation {
  val operationType = OperationTypes.Remove
}

/**
 * We separate out path into the prefix and value as once the Operation has been formed, the id in
 * path is required for creating a child unit link.
 */
case class Path(prefix: String, value: String)

object Operation {
  implicit val opWrites = new Writes[Operation] {
    override def writes(op: Operation): JsValue = op match {
      case operation: OperationWithValue => JsObject(Seq(
        "op" -> Json.toJson(operation.operationType),
        "path" -> JsString(s"${operation.path.prefix}${operation.path.value}"),
        "value" -> operation.value
      ))
      case RemoveOperation(path) => JsObject(Seq(
        "op" -> Json.toJson(op.operationType),
        "path" -> JsString(s"${path.prefix}${path.value}")
      ))
    }
  }
}