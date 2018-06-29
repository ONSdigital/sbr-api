package support.sample

import play.api.libs.json.{ JsObject, Json }
import uk.gov.ons.sbr.models.{ Ern, Lurn }

/*
 * We should be passing the local unit definition through "as-is", and hence do not require a realistic model.
 */
object SampleLocalUnit {
  def apply(ern: Ern, lurn: Lurn): String =
    s"""|{"lurn":"${lurn.value}",
        | "luref":"3325813",
        | "enterprise": {
        |   "ern":"${ern.value}",
        |   "entref":"9902232725"
        | },
        | "reportingUnit": {
        |   "rurn":"33000000063",
        |   "ruref":"49902232725"
        | },
        | "name":"BIG BOX CEREAL",
        | "address": {
        |   "line1":"(RETAIL SHOWROOM)",
        |   "line2":"KEMPSTON ST",
        |   "line5":"LIVERPOOL",
        |   "postcode":"L3 8HE"
        | },
        | "sic07":"10612",
        | "employees":42
        |}""".stripMargin

  def asJson(ern: Ern, lurn: Lurn): JsObject =
    Json.parse(SampleLocalUnit(ern, lurn)).as[JsObject]
}
