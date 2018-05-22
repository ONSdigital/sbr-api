import com.google.inject.{ AbstractModule, TypeLiteral }
import config.SbrCtrlUnitRepositoryConfigLoader
import play.api.libs.json.{ Reads, Writes }
import play.api.{ Configuration, Environment }
import repository.sbrctrl._
import repository.{ EnterpriseRepository, UnitLinksRepository }
import services.EnterpriseService
import services.sbrctrl.SbrCtrlEnterpriseService
import uk.gov.ons.sbr.models.{ LinkedUnit, UnitLinks }

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

  override def configure(): Unit = {
    val underlyingConfig = configuration.underlying
    val sbrCtrlUnitRepositoryConfig = SbrCtrlUnitRepositoryConfigLoader.load(underlyingConfig)
    bind(classOf[SbrCtrlUnitRepositoryConfig]).toInstance(sbrCtrlUnitRepositoryConfig)

    /*
     * Because of JVM type erasure, we need to use TypeLiteral to resolve generic types.
     * See: https://github.com/google/guice/wiki/FrequentlyAskedQuestions#how-to-inject-class-with-generic-type
     *      https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/TypeLiteral.html
     */
    bind(new TypeLiteral[Reads[UnitLinks]]() {}).toInstance(UnitLinks.reads)
    bind(new TypeLiteral[Writes[LinkedUnit]]() {}).toInstance(LinkedUnit.writes)

    bind(classOf[UnitRepository]).to(classOf[SbrCtrlUnitRepository])
    bind(classOf[UnitLinksRepository]).to(classOf[SbrCtrlUnitLinksRepository])
    bind(classOf[EnterpriseRepository]).to(classOf[SbrCtrlEnterpriseRepository])
    bind(classOf[EnterpriseService]).to(classOf[SbrCtrlEnterpriseService])

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
