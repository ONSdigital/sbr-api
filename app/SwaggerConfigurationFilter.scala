import java.util

import io.swagger.core.filter.SwaggerSpecFilter
import io.swagger.model.ApiDescription
import io.swagger.models.parameters.Parameter
import io.swagger.models.{ Model, Operation }
import io.swagger.models.properties.Property
/**
 * Created by haqa on 24/07/2017.
 */
class SwaggerConfigurationFilter extends SwaggerSpecFilter {

  def isParamAllowed(
    parameter: Parameter,
    operation: Operation,
    api: ApiDescription,
    params: util.Map[String, util.List[String]],
    cookies: util.Map[String, String],
    headers: util.Map[String, util.List[String]]
  ): Boolean = filter(List("none", "none"), parameter.getName)

  def isPropertyAllowed(
    model: Model,
    property: Property,
    propertyName: String,
    params: util.Map[String, util.List[String]],
    cookies: util.Map[String, String],
    headers: util.Map[String, util.List[String]]
  ): Boolean = display("none", property.getName)

  def isOperationAllowed(
    operation: Operation,
    api: ApiDescription,
    params: util.Map[String, util.List[String]],
    cookies: util.Map[String, String],
    headers: util.Map[String, util.List[String]]
  ): Boolean = true

  @deprecated("Moved to SwaggerConfigurationFilter.filter", "devops/jenkins [Mon 24 July 2017 - 11:30]")
  def display(term: String, f: => String): Boolean =
    if (f == term) false else true

  def filter(terms: List[String], f: => String): Boolean =
    !terms.map(x => if (f == x) false else true).contains(false)

}