import java.util

import io.swagger.core.filter.SwaggerSpecFilter
import io.swagger.model.ApiDescription
import io.swagger.models.parameters.Parameter
import io.swagger.models.{ Model, Operation }
import io.swagger.models.properties.Property

/**
 * SwaggerConfigurationFilter
 * ----------------
 * Author: haqa
 * Date: 24 July 2017 - 09:25
 * Copyright (c) 2017  Office for National Statistics
 */
class SwaggerConfigurationFilter extends SwaggerSpecFilter {

  private val parametersNotAllowed: List[String] = List()
  private val propertiesNotAllowed: List[String] = List()
  private val opertaionsNotAllowed: List[String] = List()

  def isParamAllowed(
    parameter: Parameter,
    operation: Operation,
    api: ApiDescription,
    params: util.Map[String, util.List[String]],
    cookies: util.Map[String, String],
    headers: util.Map[String, util.List[String]]
  ): Boolean = filter(parametersNotAllowed, parameter.getName)

  def isPropertyAllowed(
    model: Model,
    property: Property,
    propertyName: String,
    params: util.Map[String, util.List[String]],
    cookies: util.Map[String, String],
    headers: util.Map[String, util.List[String]]
  ): Boolean = filter(propertiesNotAllowed, property.getName)

  def isOperationAllowed(
    operation: Operation,
    api: ApiDescription,
    params: util.Map[String, util.List[String]],
    cookies: util.Map[String, String],
    headers: util.Map[String, util.List[String]]
  ): Boolean = true

  def filter(terms: List[String], f: => String): Boolean =
    !terms.contains(f)
}
