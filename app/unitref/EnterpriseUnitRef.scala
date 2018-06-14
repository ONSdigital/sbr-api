package unitref

import uk.gov.ons.sbr.models.UnitType.Enterprise
import uk.gov.ons.sbr.models.{ Ern, UnitId, UnitType }

object EnterpriseUnitRef extends UnitRef[Ern] {
  override def fromString(value: String): Ern =
    Ern(value)

  override def toIdTypePair(ref: Ern): (UnitId, UnitType) =
    toUnitId(ref) -> Enterprise

  override def toUnitId(ref: Ern): UnitId =
    UnitId(ref.value)
}
