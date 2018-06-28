package support.sample

import play.api.libs.json.{ JsObject, Json }
import uk.gov.ons.sbr.models.{ Ern, Rurn }

object SampleReportingUnit {
  def apply(ern: Ern, rurn: Rurn): String =
    s"""|{
        | "rurn":"${rurn.value}",
        | "ruref":"49906016135",
        | "ern":"${ern.value}",
        | "entref":"9906016135",
        | "name":"Developments Ltd",
        | "legalStatus":"1",
        | "address1":"1 Borlases Cottages",
        | "address2":"Milley Road",
        | "address3":"Waltham St Lawrence",
        | "address4":"Reading",
        | "postcode":"BR3 1HG",
        | "sic07":"47710",
        | "employees":5,
        | "employment":5,
        | "turnover":369,
        | "prn":"0.158231512"
        |}""".stripMargin

  def asJson(ern: Ern, rurn: Rurn): JsObject =
    Json.parse(SampleReportingUnit(ern, rurn)).as[JsObject]
}
