package config.providers

import com.google.inject.Provider
import javax.inject.{ Inject, Named, Singleton }
import play.api.libs.ws.WSClient
import repository.DataSourceNames.SbrCtrl
import repository.rest.{ RestUnitRepository, RestUnitRepositoryConfig, UnitRepository }

@Singleton
class SbrCtrlUnitRepositoryProvider @Inject() (@Named(SbrCtrl) restUnitRepositoryConfig: RestUnitRepositoryConfig, wSClient: WSClient) extends Provider[UnitRepository] {
  override def get(): UnitRepository = {
    new RestUnitRepository(restUnitRepositoryConfig, wSClient)
  }
}
