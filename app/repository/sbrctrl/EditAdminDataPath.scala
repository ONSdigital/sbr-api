package repository.sbrctrl

import uk.gov.ons.sbr.models.{ Period, UnitId, UnitType }

private[sbrctrl] object EditAdminDataPath {
  def apply(period: Period, unitTypeAndId: (UnitId, UnitType)): String = {
    val (unitId, unitType) = unitTypeAndId
    s"v1/periods/${Period.asString(period)}/types/${UnitType.toAcronym(unitType)}/units/${unitId.value}"
  }
}