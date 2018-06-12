import com.google.inject.name.Names
import com.google.inject.{ AbstractModule, TypeLiteral }
import config.providers.{ SbrCtrlUnitRepositoryProvider, VatAdminDataRepositoryProvider }
import config.{ BaseUrlConfigLoader, SbrCtrlRestUnitRepositoryConfigLoader, VatRestAdminDataRepositoryConfigLoader }
import handlers.LinkedUnitRetrievalHandler
import handlers.http.HttpLinkedUnitRetrievalHandler
import play.api.libs.json.{ Reads, Writes }
import play.api.mvc.Result
import play.api.{ Configuration, Environment }
import repository.DataSourceNames.{ SbrCtrl, Vat }
import repository._
import repository.rest.{ RestUnitRepositoryConfig, UnitRepository }
import repository.sbrctrl._
import services.admindata.AdminDataVatService
import services.sbrctrl.SbrCtrlEnterpriseService
import services.{ EnterpriseService, VatService }
import uk.gov.ons.sbr.models.{ AdminData, LinkedUnit, UnitLinks }

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
    val sbrCtrlRestRepositoryConfig = SbrCtrlRestUnitRepositoryConfigLoader(restRepositoryConfigLoader)(underlyingConfig)
    bind(classOf[RestUnitRepositoryConfig]).annotatedWith(Names.named(SbrCtrl)).toInstance(sbrCtrlRestRepositoryConfig)
    bind(classOf[UnitRepository]).annotatedWith(Names.named(SbrCtrl)).toProvider(classOf[SbrCtrlUnitRepositoryProvider])
    bind(classOf[UnitLinksRepository]).to(classOf[RestUnitLinksRepository])
    bind(classOf[EnterpriseRepository]).to(classOf[RestEnterpriseRepository])

    // admin data repositories
    val vatRestRepositoryConfig = VatRestAdminDataRepositoryConfigLoader(restRepositoryConfigLoader)(underlyingConfig)
    bind(classOf[RestUnitRepositoryConfig]).annotatedWith(Names.named(Vat)).toInstance(vatRestRepositoryConfig)
    bind(classOf[AdminDataRepository]).annotatedWith(Names.named(Vat)).toProvider(classOf[VatAdminDataRepositoryProvider])

    // services
    bind(classOf[EnterpriseService]).to(classOf[SbrCtrlEnterpriseService])
    bind(classOf[VatService]).to(classOf[AdminDataVatService])

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

    /*
     * Explicitly return unit to avoid warning about discarded non-Unit value.
     * This is because a .to(classOf[...]) invocation returns a ScopedBindingBuilder which is
     * currently ignored (in contrast to .toInstance(...) which returns void).
     * If such a line is the last line in this method, a ScopedBindingBuilder would be the return value of this
     * method - generating a discard warning because the method is declared to return unit.
     */
    ()
  }
}
