package support.sample

import uk.gov.ons.sbr.models._

object SampleLinkedUnit {
  def forEnterprise(period: Period, ern: Ern): LinkedUnit =
    LinkedUnit(
      UnitId(ern.value),
      UnitType.Enterprise,
      period,
      parents = None,
      children = Some(Map(UnitId("987654321") -> UnitType.LocalUnit)),
      vars = SampleEnterprise.asJson(ern)
    )
}
