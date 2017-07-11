package controllers.v1

import io.swagger.annotations.ApiModelProperty

import scala.Option

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

  def element(value: Any): String = {
    val res = value match {
      case x: String => x.toString
      case Some(z) => s"${z}"
      case None => ""
      //    case _ => ???
    }
    res
  }

  def toString(returned: List[Matches]): String = returned.map {
    case z => s"""${toMatch(z).map(x => s""""${x._1}":"${element(x._2)}"""").mkString(delim)}"""
    case _ => "Error Nothing Found"
  }.map(x => s"""{$x}""").mkString(delim)

  def fromMap(values: Array[String]) =
    Matches(values(0), Option(values(1)), Option(values(2)), Option(values(3).toLong), Option(values(4).toLong), Option(values(5)),
      Option(values(6).toLong), values(7), values(8), values(9), values(10), values(11), values(12), Option(values(13).toInt),
      Option(values(14).toInt), Option(values(15).toInt), Option(values(16).toInt), Option(values(17).toInt), Option(values(18).toLong), values(19))

}
