package support.sample

import play.api.libs.json.{ JsObject, Json }
import uk.gov.ons.sbr.models.Ern

/*
 * We should be passing the enterprise definition through "as-is", and hence do not require a realistic model.
 */
object SampleEnterprise {
  def apply(ern: Ern): String =
    s"""
       |{
       | "ern":${ern.value},
       | "employees":42,
       | "postcode":"NP10 8XG"
       |}
     """.stripMargin

  def asJson(ern: Ern): JsObject =
    Json.parse(SampleEnterprise(ern)).as[JsObject]
}
