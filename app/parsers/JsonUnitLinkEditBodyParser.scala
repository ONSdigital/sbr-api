package parsers

import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.streams.Accumulator
import play.api.mvc.Results._
import play.api.mvc.{ BodyParser, BodyParsers, RequestHeader, Result }
import uk.gov.ons.sbr.models.EditParentLink

object JsonUnitLinkEditBodyParser extends BodyParser[EditParentLink] with LazyLogging {

  val JsonPatchMediaType = "application/json-patch+json"

  override def apply(rh: RequestHeader): Accumulator[ByteString, Either[Result, EditParentLink]] = {
    BodyParsers.parse.json(rh).map { resultOrJsValue =>
      resultOrJsValue.right.flatMap(jsonToBadRequestOrEditParentLink)
    }
  }

  private def jsonToBadRequestOrEditParentLink(jsValue: JsValue): Either[Result, EditParentLink] = {
    val eitherValidationErrorOrPatch = jsValue.validate[EditParentLink].asEither
    eitherValidationErrorOrPatch.left.foreach { errors =>
      logger.error(s"Json document does not conform to EditParentLink model. Input=[$jsValue], errors=[$errors].")
    }
    eitherValidationErrorOrPatch.left.map(_ => UnprocessableEntity)
  }
}