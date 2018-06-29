package unitref

import uk.gov.ons.sbr.models.UnitType.CompaniesHouse
import uk.gov.ons.sbr.models.{ CompanyRefNumber, UnitId, UnitType }

object CompaniesHouseUnitRef extends UnitRef[CompanyRefNumber] {
  override def fromString(value: String): CompanyRefNumber =
    CompanyRefNumber(value)

  override def toIdTypePair(ref: CompanyRefNumber): (UnitId, UnitType) =
    toUnitId(ref) -> CompaniesHouse

  override def toUnitId(ref: CompanyRefNumber): UnitId =
    UnitId(ref.value)
}
