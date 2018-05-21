package config.providers

import com.google.inject.Provider
import javax.inject.{ Inject, Named, Singleton }
import play.api.libs.json.Reads
import play.api.libs.ws.WSClient
import repository.AdminDataRepository
import repository.DataSourceNames.Vat
import repository.admindata.RestAdminDataRepository
import repository.rest.{ RestUnitRepository, RestUnitRepositoryConfig }
import uk.gov.ons.sbr.models.AdminData

@Singleton
class VatAdminDataRepositoryProvider @Inject() (@Named(Vat) restUnitRepositoryConfig: RestUnitRepositoryConfig, wSClient: WSClient, readsAdminData: Reads[AdminData]) extends Provider[AdminDataRepository] {
  override def get(): AdminDataRepository = {
    val unitRepository = new RestUnitRepository(restUnitRepositoryConfig, wSClient)
    new RestAdminDataRepository(unitRepository, readsAdminData)
  }
}
