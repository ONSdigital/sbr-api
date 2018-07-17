package unitref

import uk.gov.ons.sbr.models.UnitType.ReportingUnit
import uk.gov.ons.sbr.models.{ Rurn, UnitId, UnitType }

object ReportingUnitRef extends UnitRef[Rurn] {
  override def fromUnitId(unitId: UnitId): Rurn =
    Rurn(unitId.value)

  override def toIdTypePair(ref: Rurn): (UnitId, UnitType) =
    UnitId(ref.value) -> ReportingUnit
}
