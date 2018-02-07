package uk.gov.ons.sbr.models

/**
 * DataSourceTypes
 * ----------------
 * Author: haqa
 * Date: 17 October 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
sealed trait DataSourceTypes { def path: String }

case object CRN extends DataSourceTypes {
  val path = "records"
  val referenceLabel = "CRN"
  override def toString = "CH"
}
case object VAT extends DataSourceTypes { val path = "records" }
case object PAYE extends DataSourceTypes { val path = "records" }
case object LEU extends DataSourceTypes { val path = "records" }
case object ENT extends DataSourceTypes { val path = "enterprises" }

// create DataSourceTypes.type from str
object DataSourceTypesUtil {
  def fromString(value: String): Option[DataSourceTypes] = {
    Vector(CRN, VAT, PAYE, LEU, ENT).find(_.toString.equalsIgnoreCase(value))
  }

  // returns unit reference name for CH
  def converter(unit: String): String = unit match {
    case x if x == CRN.toString => CRN.referenceLabel
    case x => x
  }
}
