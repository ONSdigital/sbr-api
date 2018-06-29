package unitref

import uk.gov.ons.sbr.models.UnitType.Enterprise
import uk.gov.ons.sbr.models.{ Ern, UnitId, UnitType }

object EnterpriseUnitRef extends UnitRef[Ern] {
  override def fromUnitId(unitId: UnitId): Ern =
    Ern(unitId.value)

  override def toIdTypePair(ref: Ern): (UnitId, UnitType) =
    UnitId(ref.value) -> Enterprise
}
