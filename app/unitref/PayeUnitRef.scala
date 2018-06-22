package unitref

import uk.gov.ons.sbr.models.UnitType.PayAsYouEarn
import uk.gov.ons.sbr.models.{ PayeRef, UnitId, UnitType }

object PayeUnitRef extends UnitRef[PayeRef] {
  override def fromUnitId(unitId: UnitId): PayeRef =
    PayeRef(unitId.value)

  override def toIdTypePair(ref: PayeRef): (UnitId, UnitType) =
    UnitId(ref.value) -> PayAsYouEarn
}
