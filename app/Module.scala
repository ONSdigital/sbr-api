import actions.RetrieveLinkedUnitAction
import actions.RetrieveLinkedUnitAction.LinkedUnitTracedRequestActionFunctionMaker
import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import com.typesafe.scalalogging.LazyLogging
import config.{BaseUrlConfigLoader, RestAdminDataRepositoryConfigLoader, SbrCtrlRestUnitRepositoryConfigLoader}
import handlers.LinkedUnitRetrievalHandler
import handlers.http.HttpLinkedUnitRetrievalHandler
import javax.inject.{Inject, Named, Singleton}
import parsers.JsonUnitLinkEditBodyParser
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{BodyParser, PlayBodyParsers, Result}
import play.api.{Configuration, Environment}
import repository.DataSourceNames.{CompaniesHouse, Paye, SbrCtrl, Vat}
import repository._
import repository.admindata.RestAdminDataRepository
import repository.rest.{Repository, RestRepository, RestRepositoryConfig}
import repository.sbrctrl._
import services._
import services.finder.{AdminDataFinder, ByParentEnterpriseUnitFinder, EnterpriseFinder}
import tracing.TraceWSClient
import uk.gov.ons.sbr.models._
import unitref._

import scala.concurrent.ExecutionContext

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
) extends AbstractModule with LazyLogging {
  /*
   * Note that we use a common class as a REST repository, of which we need multiple instances - all configured
   * differently.  For example, in addition to communicating with sbr-control-api, we also need to communicate
   * with "admin data" - but there are actually three different sources of "admin data", each of which is an
   * independent deployment.
   * As we do not have unique types across all of these required instances, we rely on the javax.inject.Named
   * annotation.  Injection sites must specify the "name" of the instance they need to be made available to them.
   */
  override def configure(): Unit = {
    logger.info(s"Configuring application for environment mode [${environment.mode}]")
    val underlyingConfig = configuration.underlying
    val restRepositoryConfigLoader = BaseUrlConfigLoader.map(RestRepositoryConfig)

    // sbr repositories
    bind(classOf[RestRepositoryConfig]).annotatedWith(named(SbrCtrl)).toInstance(
      SbrCtrlRestUnitRepositoryConfigLoader(restRepositoryConfigLoader, underlyingConfig)
    )
    bind(classOf[UnitLinksRepository]).to(classOf[RestUnitLinksRepository])
    bind(classOf[EnterpriseRepository]).to(classOf[RestEnterpriseRepository])
    bind(classOf[LocalUnitRepository]).to(classOf[RestLocalUnitRepository])
    bind(classOf[ReportingUnitRepository]).to(classOf[RestReportingUnitRepository])

    // config for admin data repositories
    bind(classOf[RestRepositoryConfig]).annotatedWith(named(Vat)).toInstance(
      RestAdminDataRepositoryConfigLoader.vat(restRepositoryConfigLoader, underlyingConfig)
    )
    bind(classOf[RestRepositoryConfig]).annotatedWith(named(Paye)).toInstance(
      RestAdminDataRepositoryConfigLoader.paye(restRepositoryConfigLoader, underlyingConfig)
    )
    bind(classOf[RestRepositoryConfig]).annotatedWith(named(CompaniesHouse)).toInstance(
      RestAdminDataRepositoryConfigLoader.companiesHouse(restRepositoryConfigLoader, underlyingConfig)
    )

    // Edit -> Patch Conversion
    bind(classOf[EditService]).to(classOf[UnitLinksEditService])
    bind(classOf[AdminDataUnitLinksEditRepository]).to(classOf[RestAdminDataUnitLinksEditRepository])

    // generics
    /*
     * Because of JVM type erasure, we need to use TypeLiteral to resolve generic types.
     * See: https://github.com/google/guice/wiki/FrequentlyAskedQuestions#how-to-inject-class-with-generic-type
     *      https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/TypeLiteral.html
     */
    bind(new TypeLiteral[Reads[UnitLinks]]() {}).toInstance(UnitLinks.reads)
    bind(new TypeLiteral[Reads[AdminData]]() {}).toInstance(AdminData.reads)
    bind(new TypeLiteral[Writes[LinkedUnit]]() {}).toInstance(LinkedUnit.writes)

    bind(new TypeLiteral[UnitRef[CompanyRefNumber]]() {}).toInstance(CompaniesHouseUnitRef)
    bind(new TypeLiteral[UnitRef[Ern]]() {}).toInstance(EnterpriseUnitRef)
    bind(new TypeLiteral[UnitRef[Lurn]]() {}).toInstance(LocalUnitRef)
    bind(new TypeLiteral[UnitRef[Rurn]]() {}).toInstance(ReportingUnitRef)
    bind(new TypeLiteral[UnitRef[PayeRef]]() {}).toInstance(PayeUnitRef)
    bind(new TypeLiteral[UnitRef[VatRef]]() {}).toInstance(VatUnitRef)

    bind(new TypeLiteral[LinkedUnitRetrievalHandler[Result]]() {}).to(classOf[HttpLinkedUnitRetrievalHandler])

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
  def providesSbrCtrlUnitRepository(@Inject()@Named(SbrCtrl) sbrCtrlUnitRepositoryConfig: RestRepositoryConfig, wsClient: TraceWSClient): Repository =
    new RestRepository(sbrCtrlUnitRepositoryConfig, wsClient)

  @Provides @Named(Vat)
  def providesVatAdminDataRepository(@Inject()@Named(Vat) vatUnitRepositoryConfig: RestRepositoryConfig, wsClient: TraceWSClient, readsAdminData: Reads[AdminData]): AdminDataRepository = {
    val unitRepository = new RestRepository(vatUnitRepositoryConfig, wsClient)
    new RestAdminDataRepository(unitRepository, readsAdminData, Vat)
  }

  @Provides @Named(Paye)
  def providesPayeAdminDataRepository(@Inject()@Named(Paye) payeUnitRepositoryConfig: RestRepositoryConfig, wsClient: TraceWSClient, readsAdminData: Reads[AdminData]): AdminDataRepository = {
    val unitRepository = new RestRepository(payeUnitRepositoryConfig, wsClient)
    new RestAdminDataRepository(unitRepository, readsAdminData, Paye)
  }

  @Provides @Named(CompaniesHouse)
  def providesCompaniesHouseAdminDataRepository(@Inject()@Named(CompaniesHouse) chUnitRepositoryConfig: RestRepositoryConfig, wsClient: TraceWSClient, readsAdminData: Reads[AdminData]): AdminDataRepository = {
    val unitRepository = new RestRepository(chUnitRepositoryConfig, wsClient)
    new RestAdminDataRepository(unitRepository, readsAdminData, CompaniesHouse)
  }

  // services
  @Provides
  def providesVatService(@Inject() unitRefType: UnitRef[VatRef], unitLinksRepository: UnitLinksRepository,
    @Named(Vat) vatRepository: AdminDataRepository): LinkedUnitService[VatRef] = {
    val vatFinder = new AdminDataFinder[VatRef](unitRefType, vatRepository)
    new RestLinkedUnitService[VatRef](unitRefType, unitLinksRepository, vatFinder)
  }

  @Provides
  def providesPayeService(@Inject() unitRefType: UnitRef[PayeRef], unitLinksRepository: UnitLinksRepository,
    @Named(Paye) payeRepository: AdminDataRepository): LinkedUnitService[PayeRef] = {
    val payeFinder = new AdminDataFinder[PayeRef](unitRefType, payeRepository)
    new RestLinkedUnitService[PayeRef](unitRefType, unitLinksRepository, payeFinder)
  }

  @Provides
  def providesCompaniesHouseService(@Inject() unitRefType: UnitRef[CompanyRefNumber], unitLinksRepository: UnitLinksRepository,
    @Named(CompaniesHouse) chRepository: AdminDataRepository): LinkedUnitService[CompanyRefNumber] = {
    val chFinder = new AdminDataFinder[CompanyRefNumber](unitRefType, chRepository)
    new RestLinkedUnitService[CompanyRefNumber](unitRefType, unitLinksRepository, chFinder)
  }

  @Provides
  def providesEnterpriseService(@Inject() enterpriseRefType: UnitRef[Ern], unitLinksRepository: UnitLinksRepository,
    enterpriseRepository: EnterpriseRepository): LinkedUnitService[Ern] = {
    val enterpriseFinder = new EnterpriseFinder(enterpriseRepository)
    new RestLinkedUnitService[Ern](enterpriseRefType, unitLinksRepository, enterpriseFinder)
  }

  @Provides
  def providesLocalUnitService(@Inject() localUnitRefType: UnitRef[Lurn], enterpriseUnitRefType: UnitRef[Ern],
    unitLinksRepository: UnitLinksRepository, localUnitRepository: LocalUnitRepository): LinkedUnitService[Lurn] = {
    val localUnitFinder = new ByParentEnterpriseUnitFinder[Lurn](localUnitRepository.retrieveLocalUnit, enterpriseUnitRefType)
    new RestLinkedUnitService[Lurn](localUnitRefType, unitLinksRepository, localUnitFinder)
  }

  @Provides
  def providesReportingUnitService(@Inject() reportingUnitRefType: UnitRef[Rurn], enterpriseUnitRefType: UnitRef[Ern],
    unitLinksRepository: UnitLinksRepository, reportingUnitRepository: ReportingUnitRepository): LinkedUnitService[Rurn] = {
    val reportingUnitFinder = new ByParentEnterpriseUnitFinder[Rurn](reportingUnitRepository.retrieveReportingUnit, enterpriseUnitRefType)
    new RestLinkedUnitService[Rurn](reportingUnitRefType, unitLinksRepository, reportingUnitFinder)
  }

  // controller actions
  @Provides
  def providesEnterpriseLinkedUnitRequestActionBuilderMaker(@Inject() enterpriseService: LinkedUnitService[Ern], ec: ExecutionContext): LinkedUnitTracedRequestActionFunctionMaker[Ern] =
    new RetrieveLinkedUnitAction[Ern](enterpriseService, ec)

  @Provides
  def providesLocalLinkedUnitRequestActionBuilderMaker(@Inject() localUnitService: LinkedUnitService[Lurn], ec: ExecutionContext): LinkedUnitTracedRequestActionFunctionMaker[Lurn] =
    new RetrieveLinkedUnitAction[Lurn](localUnitService, ec)

  @Provides
  def providesReportingLinkedUnitRequestActionBuilderMaker(@Inject() reportingUnitService: LinkedUnitService[Rurn], ec: ExecutionContext): LinkedUnitTracedRequestActionFunctionMaker[Rurn] =
    new RetrieveLinkedUnitAction[Rurn](reportingUnitService, ec)

  @Provides
  def providesVatLinkedUnitRequestActionBuilderMaker(@Inject() vatService: LinkedUnitService[VatRef], ec: ExecutionContext): LinkedUnitTracedRequestActionFunctionMaker[VatRef] =
    new RetrieveLinkedUnitAction[VatRef](vatService, ec)

  @Provides
  def providesPayeLinkedUnitRequestActionBuilderMaker(@Inject() payeService: LinkedUnitService[PayeRef], ec: ExecutionContext): LinkedUnitTracedRequestActionFunctionMaker[PayeRef] =
    new RetrieveLinkedUnitAction[PayeRef](payeService, ec)

  @Provides
  def providesCompaniesHouseLinkedUnitRequestActionBuilderMaker(@Inject() chService: LinkedUnitService[CompanyRefNumber], ec: ExecutionContext): LinkedUnitTracedRequestActionFunctionMaker[CompanyRefNumber] =
    new RetrieveLinkedUnitAction[CompanyRefNumber](chService, ec)
  
  // body parsers
  @Provides
  @Singleton
  def providesEditParentLinkBodyParser(@Inject() bodyParsers: PlayBodyParsers, ec: ExecutionContext): BodyParser[EditParentLink] =
    new JsonUnitLinkEditBodyParser(bodyParsers.json)(ec)
}