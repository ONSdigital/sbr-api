package controllers.v1

import io.swagger.annotations.ApiModelProperty

/**
 * Created by haqa on 07/07/2017.
 */
final case class Matches(
  @ApiModelProperty(value = "", example = "", required = false, hidden = false) name: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") enterprise: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") paye: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") vatref: Option[Long],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") ubrn: Option[Long],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.String") crn: Option[String],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") idbr: Option[Long],
  @ApiModelProperty(value = "", example = "") address1: String,
  @ApiModelProperty(value = "", example = "") address2: String,
  @ApiModelProperty(value = "", example = "") address3: String,
  @ApiModelProperty(value = "", example = "") address4: String,
  @ApiModelProperty(value = "", example = "") address5: String,
  @ApiModelProperty(value = "", example = "") postcode: String,
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") legalStatus: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") sic: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employees: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") workingGroup: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Integer") employment: Option[Int],
  @ApiModelProperty(value = "", example = "", dataType = "java.lang.Long") turnover: Option[Long],
  @ApiModelProperty(value = "", example = "") source: String
)

object MatchObj {

  private val delim: String = ","

  def toMatch(m: Matches) = Map(
    "name" -> m.name,
    "enterprise" -> m.enterprise,
    "paye" -> m.paye,
    "vatref" -> m.vatref,
    "ubrn" -> m.ubrn,
    "crn" -> m.crn,
    "idbr" -> m.idbr,
    "address1" -> m.address1,
    "address2" -> m.address2,
    "address3" -> m.address3,
    "address4" -> m.address4,
    "address5" -> m.address5,
    "postcode" -> m.postcode,
    "legalStatus" -> m.legalStatus,
    "sic" -> m.sic,
    "employees" -> m.employees,
    "workingGroup" -> m.workingGroup,
    "employment" -> m.employment,
    "turnover" -> m.turnover,
    "source" -> m.source
  )

  def element[T](value: T): String = value match {
    case x => x.toString
    case Some(z) => s"${z}"
    case None => ""
    //    case _ => ???
  }

  def toString(returned: List[Matches]): String = returned.map {
    //    case z => toMatch(z)
    //     map(toMatch(z) => println(x))
    case z =>
      toMatch(z).map(x => println(s"xs: ${x}}"));
      s""""name":"${z.name}","enterprise":"${element(z.enterprise)}","paye":"${element(z.paye)}","vatref":"${element(z.vatref)}", "ubrn":"${element(z.ubrn)}", "crn":"${element(z.crn)}", "idbr":"${element(z.idbr)}", "address1":"${z.address1}", "address2":"${z.address2}", "address3":"${z.address3}", "address4":"${z.address4}", "address5":"${z.address5}", "postcode":"${z.postcode}", "legelStatus":"${element(z.legalStatus)}", "sic":"${element(z.sic)}", "employees":"${element(z.employees)}", "workingGroup":"${element(z.workingGroup)}", "employment":"${element(z.employment)}", "turnover":"${element(z.turnover)}", "source":"${z.source}""""
    case _ => "Error Nothing Found"
  }.map(x => s"""{$x}""").mkString(delim)

  //  def fromMap(values: Array[String]) =
  //    Matches(values(0), values(1), values(2), values(3).toLong, values(4).toLong, values(5), values(6).toLong, values(7), values(8),
  //      values(9), values(10), values(11), values(12), values(13).toInt, values(14).toInt, values(15).toInt, values(16).toInt,
  //      values(17).toInt, values(18).toLong, values(19))

  def fromMap(values: Array[String]) =
    Matches(values(0), Option(values(1)), Option(values(2)), Option(values(3).toLong), Option(values(4).toLong), Option(values(5)),
      Option(values(6).toLong), values(7), values(8), values(9), values(10), values(11), values(12), Option(values(13).toInt),
      Option(values(14).toInt), Option(values(15).toInt), Option(values(16).toInt), Option(values(17).toInt), Option(values(18).toLong), values(19))

}
