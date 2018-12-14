/**
 * Copyright (c) 2016-2017 in alphabetical order:
 * Atos IT Solutions and Services GmbH, National University of Ireland Galway, Siemens AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package exchange.api

import org.scalatest.{FlatSpec, Matchers}

import exchange.api.semantics._

class DataFieldSpec extends FlatSpec with Matchers with SemanticsFixtures {
  "DataFieldInput with no name" should "validate" in {
    val input = numberDataFieldInput.copy(name = None)

    input.validate(Nil, default = numberDataField) shouldBe None
  }

  it should "use default value type" in {
    val input = numberDataFieldInput.copy(name = None)

    val dataField = input.toDataField(default = numberDataField)

    dataField.isDefined shouldBe true
    dataField.get.name shouldBe name
  }

  "DataFieldInput with no value type" should "validate" in {
    val input = numberDataFieldInput.copy(value = None)

    input.validate(Nil, default = numberDataField) shouldBe None
  }

  it should "use default value type" in {
    val input = numberDataFieldInput.copy(value = None)

    val dataField = input.toDataField(default = numberDataField)

    dataField.isDefined shouldBe true
    dataField.get.value shouldBe numberType
  }

  "DataFieldInput with no default value type" should "validate" in {
    numberDataFieldInput.validate(Nil, default = None) shouldBe None
  }

  it should "use provided value type" in {
    val dataField = numberDataFieldInput.toDataField(default = None)

    dataField.isDefined shouldBe true
    dataField.get.value.`type` shouldBe numberDataFieldInput.value.get.`type`
  }

  "DataFieldInput with provided encodingType" should "validate" in {
    val input = numberDataFieldInput.copy(encodingType = encodingType)

    input.validate(Nil, default = numberDataField) shouldBe None
  }

  it should "use that encodingType" in {
    val input = numberDataFieldInput.copy(encodingType = encodingType)

    val dataField = input.toDataField(default = numberDataField)

    dataField.isDefined shouldBe true
    dataField.get.encodingType shouldBe encodingType
  }

  "DataFieldInput with provided 'required' property" should "validate" in {
    val input = numberDataFieldInput.copy(required = required)

    input.validate(Nil, default = numberDataField) shouldBe None
  }

  it should "use that required property" in {
    val input = numberDataFieldInput.copy(required = required)

    val dataField = input.toDataField(default = numberDataField)

    dataField.isDefined shouldBe true
    dataField.get.required shouldBe required
  }

  "DataFieldInput with text type instead of number value type" should "validate" in {
    val input = numberDataFieldInput.copy(value = Some(textTypeInput))

    input.validate(Nil, default = numberDataField) shouldBe empty
  }

  it should "use text type" in {
    val input = numberDataFieldInput.copy(value = Some(textTypeInput))

    val dataField = input.toDataField(default = numberDataField)

    dataField.isDefined shouldBe true
    dataField.get.value.`type` shouldBe Text
  }

  "DataFieldInput with object instead of number value type" should "not validate" in {
    val input = numberDataFieldInput.copy(value = Some(objectTypeInput))

    input.validate(Nil, default = numberDataField) shouldBe defined
  }

}
