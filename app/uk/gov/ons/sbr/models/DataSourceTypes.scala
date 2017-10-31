package uk.gov.ons.sbr.models

/**
 * Created by haqa on 17/10/2017.
 */
sealed trait DataSourceTypes { def path: String }

case object CRN extends DataSourceTypes { val path = "companies"; val shortName: String = "CH" }
case object VAT extends DataSourceTypes { val path = "vats" }
case object PAYE extends DataSourceTypes { val path = "payes" }
case object LEU extends DataSourceTypes { val path = "business" }
case object ENT extends DataSourceTypes { val path = "enterprises" }

// create DataSourceTypes.type from str
object DataSourceTypesUtil {
  def fromString(value: String): Option[DataSourceTypes] = {
    Vector(CRN, VAT, PAYE, LEU, ENT).find(_.toString == value)
  }
}
