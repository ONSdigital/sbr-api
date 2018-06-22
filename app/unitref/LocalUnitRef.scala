package unitref

import uk.gov.ons.sbr.models.UnitType.LocalUnit
import uk.gov.ons.sbr.models.{ Lurn, UnitId, UnitType }

object LocalUnitRef extends UnitRef[Lurn] {
  override def fromUnitId(unitId: UnitId): Lurn =
    Lurn(unitId.value)

  override def toIdTypePair(ref: Lurn): (UnitId, UnitType) =
    UnitId(ref.value) -> LocalUnit
}
