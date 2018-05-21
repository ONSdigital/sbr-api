package support.sample

import play.api.libs.json.{ JsObject, Json }
import uk.gov.ons.sbr.models.VatRef

/*
 * We should be passing the VAT admin data variables through "as-is", and hence do not require a realistic model.
 */
object SampleVat {
  def apply(vatref: VatRef): String =
    s"""|{
        | "entref":"1000012345",
        | "vatref":"${vatref.value}",
        | "sic92":"73120",
        | "postcode":"NG14 5AR"
        |}""".stripMargin

  def asJson(vatRef: VatRef): JsObject =
    Json.parse(SampleVat(vatRef)).as[JsObject]
}
