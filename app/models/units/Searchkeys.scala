package models.units

import com.google.inject.ImplementedBy
import io.swagger.annotations.ApiModelProperty
import models.units.attributes.Address

/**
 * Created by haqa on 11/07/2017.
 */
@ImplementedBy(classOf[Enterprise])
trait Searchkeys[T] {
  @ApiModelProperty(value = "A searchable identifier", required = true, hidden = false) def id: T
  @ApiModelProperty(value = "An address object consisting of 5 line descriptors") def address: Address
  @ApiModelProperty(value = "A post specific to the address of entity") def postcode: String
  @ApiModelProperty(value = "Data provider") def source: String
}

