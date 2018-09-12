package parsers

import akka.util.ByteString
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.streams.Accumulator
import play.api.mvc.{ BodyParser, BodyParsers, RequestHeader, Result }
import uk.gov.ons.sbr.models.EditParentLink

object JsonUnitLinkEditBodyParser extends BodyParser[EditParentLink] {
  override def apply(rh: RequestHeader): Accumulator[ByteString, Either[Result, EditParentLink]] = {
    BodyParsers.parse.json(rh).map { resultOrJsValue =>
      resultOrJsValue.right.map(_.as[EditParentLink])
    }
  }
}