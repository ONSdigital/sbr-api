import actions.RetrieveLinkedUnitAction
import actions.RetrieveLinkedUnitAction.LinkedUnitRequestActionBuilderMaker
import com.google.inject.name.Names.named
import com.google.inject.{ AbstractModule, Provides, TypeLiteral }
import config.{ BaseUrlConfigLoader, RestAdminDataRepositoryConfigLoader, SbrCtrlRestUnitRepositoryConfigLoader }
import handlers.LinkedUnitRetrievalHandler
import handlers.http.HttpLinkedUnitRetrievalHandler
import javax.inject.{ Inject, Named }
import play.api.libs.json.{ Reads, Writes }
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import play.api.{ Configuration, Environment }
import repository.DataSourceNames.{ CompaniesHouse, Paye, SbrCtrl, Vat }
import repository._
import repository.admindata.RestAdminDataRepository
import repository.rest.{ RestUnitRepository, RestUnitRepositoryConfig, UnitRepository }
import repository.sbrctrl._
import services.LinkedUnitService
import services.admindata.AdminDataService
import services.sbrctrl.SbrCtrlEnterpriseService
import uk.gov.ons.sbr.models._
import unitref.{ CompaniesHouseUnitRef, PayeUnitRef, VatUnitRef }

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module(
    environment: Environment,
    configuration: Configuration
) extends AbstractModule {
  /*
   * Note that we use a common class as a REST repository, of which we need multiple instances - all configured
   * differently.  For example, in addition to communicating with sbr-control-api, we also need to communicate
   * with "admin data" - but there are actually three different sources of "admin data", each of which is an
   * independent deployment.
   * As we do not have unique types across all of these required instances, we rely on the javax.inject.Named
   * annotation.  Injection sites must specify the "name" of the instance they need to be made available to them.
   */
  override def configure(): Unit = {
    val underlyingConfig = configuration.underlying
    val restRepositoryConfigLoader = BaseUrlConfigLoader.map(RestUnitRepositoryConfig)

    // sbr repositories
    bind(classOf[RestUnitRepositoryConfig]).annotatedWith(named(SbrCtrl)).toInstance(
      SbrCtrlRestUnitRepositoryConfigLoader(restRepositoryConfigLoader, underlyingConfig)
    )
    bind(classOf[UnitLinksRepository]).to(classOf[RestUnitLinksRepository])
    bind(classOf[EnterpriseRepository]).to(classOf[RestEnterpriseRepository])

    // config for admin data repositories
    bind(classOf[RestUnitRepositoryConfig]).annotatedWith(named(Vat)).toInstance(
      RestAdminDataRepositoryConfigLoader.vat(restRepositoryConfigLoader, underlyingConfig)
    )
    bind(classOf[RestUnitRepositoryConfig]).annotatedWith(named(Paye)).toInstance(
      RestAdminDataRepositoryConfigLoader.paye(restRepositoryConfigLoader, underlyingConfig)
    )
    bind(classOf[RestUnitRepositoryConfig]).annotatedWith(named(CompaniesHouse)).toInstance(
      RestAdminDataRepositoryConfigLoader.companiesHouse(restRepositoryConfigLoader, underlyingConfig)
    )

    // generics
    /*
     * Because of JVM type erasure, we need to use TypeLiteral to resolve generic types.
     * See: https://github.com/google/guice/wiki/FrequentlyAskedQuestions#how-to-inject-class-with-generic-type
     *      https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/TypeLiteral.html
     */
    bind(new TypeLiteral[Reads[UnitLinks]]() {}).toInstance(UnitLinks.reads)
    bind(new TypeLiteral[Reads[AdminData]]() {}).toInstance(AdminData.reads)
    bind(new TypeLiteral[Writes[LinkedUnit]]() {}).toInstance(LinkedUnit.writes)
    bind(new TypeLiteral[LinkedUnitRetrievalHandler[Result]]() {}).to(classOf[HttpLinkedUnitRetrievalHandler])
    bind(new TypeLiteral[LinkedUnitService[Ern]]() {}).to(classOf[SbrCtrlEnterpriseService])

    /*
     * Explicitly return unit to avoid warning about discarded non-Unit value.
     * This is because a .to(classOf[...]) invocation returns a ScopedBindingBuilder which is
     * currently ignored (in contrast to .toInstance(...) which returns void).
     * If such a line is the last line in this method, a ScopedBindingBuilder would be the return value of this
     * method - generating a discard warning because the method is declared to return unit.
     */
    ()
  }

  // repositories
  @Provides @Named(SbrCtrl)
  def providesSbrCtrlUnitRepository(@Inject()@Named(SbrCtrl) sbrCtrlUnitRepositoryConfig: RestUnitRepositoryConfig, wSClient: WSClient): UnitRepository =
    new RestUnitRepository(sbrCtrlUnitRepositoryConfig, wSClient)

  @Provides @Named(Vat)
  def providesVatAdminDataRepository(@Inject()@Named(Vat) vatUnitRepositoryConfig: RestUnitRepositoryConfig, wSClient: WSClient, readsAdminData: Reads[AdminData]): AdminDataRepository = {
    val unitRepository = new RestUnitRepository(vatUnitRepositoryConfig, wSClient)
    new RestAdminDataRepository(unitRepository, readsAdminData)
  }

  @Provides @Named(Paye)
  def providesPayeAdminDataRepository(@Inject()@Named(Paye) payeUnitRepositoryConfig: RestUnitRepositoryConfig, wSClient: WSClient, readsAdminData: Reads[AdminData]): AdminDataRepository = {
    val unitRepository = new RestUnitRepository(payeUnitRepositoryConfig, wSClient)
    new RestAdminDataRepository(unitRepository, readsAdminData)
  }

  @Provides @Named(CompaniesHouse)
  def providesCompaniesHouseAdminDataRepository(@Inject()@Named(CompaniesHouse) chUnitRepositoryConfig: RestUnitRepositoryConfig, wSClient: WSClient, readsAdminData: Reads[AdminData]): AdminDataRepository = {
    val unitRepository = new RestUnitRepository(chUnitRepositoryConfig, wSClient)
    new RestAdminDataRepository(unitRepository, readsAdminData)
  }

  // services
  @Provides
  def providesVatService(@Inject() unitLinksRepository: UnitLinksRepository, @Named(Vat) vatRepository: AdminDataRepository): LinkedUnitService[VatRef] =
    new AdminDataService[VatRef](VatUnitRef, unitLinksRepository, vatRepository)

  @Provides
  def providesPayeService(@Inject() unitLinksRepository: UnitLinksRepository, @Named(Paye) payeRepository: AdminDataRepository): LinkedUnitService[PayeRef] =
    new AdminDataService[PayeRef](PayeUnitRef, unitLinksRepository, payeRepository)

  @Provides
  def providesCompaniesHouseService(@Inject() unitLinksRepository: UnitLinksRepository, @Named(CompaniesHouse) chRepository: AdminDataRepository): LinkedUnitService[CompanyRefNumber] =
    new AdminDataService[CompanyRefNumber](CompaniesHouseUnitRef, unitLinksRepository, chRepository)

  // controller actions
  @Provides
  def providesEnterpriseLinkedUnitRequestActionBuilderMaker(@Inject() enterpriseService: LinkedUnitService[Ern]): LinkedUnitRequestActionBuilderMaker[Ern] =
    new RetrieveLinkedUnitAction[Ern](enterpriseService)

  @Provides
  def providesVatLinkedUnitRequestActionBuilderMaker(@Inject() vatService: LinkedUnitService[VatRef]): LinkedUnitRequestActionBuilderMaker[VatRef] =
    new RetrieveLinkedUnitAction[VatRef](vatService)

  @Provides
  def providesPayeLinkedUnitRequestActionBuilderMaker(@Inject() payeService: LinkedUnitService[PayeRef]): LinkedUnitRequestActionBuilderMaker[PayeRef] =
    new RetrieveLinkedUnitAction[PayeRef](payeService)

  @Provides
  def providesCompaniesHouseLinkedUnitRequestActionBuilderMaker(@Inject() chService: LinkedUnitService[CompanyRefNumber]): LinkedUnitRequestActionBuilderMaker[CompanyRefNumber] =
    new RetrieveLinkedUnitAction[CompanyRefNumber](chService)
}
