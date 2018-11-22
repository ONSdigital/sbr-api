package parsers

import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.libs.streams.Accumulator
import play.api.mvc.Results._
import play.api.mvc.{BodyParser, RequestHeader, Result}
import uk.gov.ons.sbr.models.EditParentLink

import scala.concurrent.ExecutionContext

class JsonUnitLinkEditBodyParser @Inject() (jsonBodyParser: BodyParser[JsValue])(implicit ec: ExecutionContext) extends BodyParser[EditParentLink] with LazyLogging {
  override def apply(rh: RequestHeader): Accumulator[ByteString, Either[Result, EditParentLink]] =
    jsonBodyParser(rh).map { resultOrJsValue =>
      resultOrJsValue.right.flatMap(jsonToBadRequestOrEditParentLink)
    }

  private def jsonToBadRequestOrEditParentLink(jsValue: JsValue): Either[Result, EditParentLink] = {
    val eitherValidationErrorOrPatch = jsValue.validate[EditParentLink].asEither
    eitherValidationErrorOrPatch.left.foreach { errors =>
      logger.error(s"Json document does not conform to EditParentLink model. Input=[$jsValue], errors=[$errors].")
    }
    eitherValidationErrorOrPatch.left.map(_ => UnprocessableEntity)
  }
}

object JsonUnitLinkEditBodyParser {
  val JsonPatchMediaType = "application/json-patch+json"
}