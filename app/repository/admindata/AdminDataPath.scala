package repository.admindata

import uk.gov.ons.sbr.models.{ Period, UnitId }

private[admindata] object AdminDataPath {
  def apply(unitId: UnitId, period: Period): String =
    s"v1/records/${unitId.value}/periods/${Period.asString(period)}"
}
