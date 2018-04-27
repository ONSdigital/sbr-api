package repository.sbrctrl

import uk.gov.ons.sbr.models.{ Period, UnitId, UnitType }

private[sbrctrl] object UnitLinksPath {
  def apply(unitId: UnitId, unitType: UnitType, period: Period): String = {
    val unitTypeAcronym = UnitType.toAcronym(unitType)
    val periodStr = Period.asString(period)
    s"v1/periods/$periodStr/types/$unitTypeAcronym/units/${unitId.value}"
  }
}
