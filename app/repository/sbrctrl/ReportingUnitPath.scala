package repository.sbrctrl

import uk.gov.ons.sbr.models.{ Ern, Period, Rurn }

private[sbrctrl] object ReportingUnitPath {
  def apply(period: Period, ern: Ern, rurn: Rurn): String =
    s"v1/enterprises/${ern.value}/periods/${Period.asString(period)}/reportingunits/${rurn.value}"
}
