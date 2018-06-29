package unitref

import uk.gov.ons.sbr.models.UnitType.CompaniesHouse
import uk.gov.ons.sbr.models.{ CompanyRefNumber, UnitId, UnitType }

object CompaniesHouseUnitRef extends UnitRef[CompanyRefNumber] {
  override def fromUnitId(unitId: UnitId): CompanyRefNumber =
    CompanyRefNumber(unitId.value)

  override def toIdTypePair(ref: CompanyRefNumber): (UnitId, UnitType) =
    UnitId(ref.value) -> CompaniesHouse
}
