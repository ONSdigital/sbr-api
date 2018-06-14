package support.sample

import play.api.libs.json.{ JsObject, Json }
import uk.gov.ons.sbr.models.PayeRef

/*
 * We should be passing the PAYE admin data variables through "as-is", and hence do not require a realistic model.
 */
object SamplePaye {
  def apply(payeRef: PayeRef): String =
    s"""
       |{
       | "entref":"6253363711",
       | "payeref":"${payeRef.value}",
       | "legalstatus":"A",
       | "dec_jobs":42,
       | "postcode":"LR49 ZWG"
       |}""".stripMargin

  def asJson(payeRef: PayeRef): JsObject =
    Json.parse(SamplePaye(payeRef)).as[JsObject]
}
