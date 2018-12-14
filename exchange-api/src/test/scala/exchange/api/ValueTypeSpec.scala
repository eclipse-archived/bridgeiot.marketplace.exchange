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

class ValueTypeSpec extends FlatSpec with Matchers with SemanticsFixtures {
  "Object type input with number type default" should "not validate" in {
    objectTypeInput.validate(Nil, default = Some(numberType)) shouldBe defined
  }

  "Object type input with missing member list" should "validate" in {
    val reqObjectTypeInput = ValueTypeInput(Obj, members = None)

    reqObjectTypeInput.validate(Nil, default = Some(objectType)) shouldBe empty
  }

  it should "contain all default members" in {
    val reqObjectTypeInput = ValueTypeInput(Obj, members = None)

    val valueType = reqObjectTypeInput.toValueType(default = Some(objectType))

    valueType shouldBe defined
    valueType.get.`type` shouldBe Obj
    valueType.get.asInstanceOf[ObjectType].members.map(_.name) should contain only (sub1Name, sub2Name)
  }

  "Object type input with empty member list" should "not validate" in {
    val reqObjectTypeInput = ValueTypeInput(Obj, members = Some(Nil))

    reqObjectTypeInput.validate(Nil, default = Some(objectType)) shouldBe defined
  }

  "Object type input with incomplete member list" should "validate" in {
    val reqObjectTypeInput = ValueTypeInput(Obj, members = Some(List(sub1DataFieldInput)))

    reqObjectTypeInput.validate(Nil, default = Some(objectType)) shouldBe empty
  }

  it should "only override provided members" in {
    val reqObjectTypeInput = ValueTypeInput(Obj, members = Some(List(sub1DataFieldInput)))

    val valueType = reqObjectTypeInput.toValueType(default = Some(objectType))

    valueType shouldBe defined
    valueType.get.`type` shouldBe Obj
    valueType.get.asInstanceOf[ObjectType].members.map(_.name) should contain only sub1Name
    valueType.get.asInstanceOf[ObjectType].members.map(_.value.`type`) should contain only sub1DataField.value.`type`
  }

  "Object type input with out-of-order member list" should "validate" in {
    val reqObjectTypeInput = ValueTypeInput(Obj, members = Some(List(sub2DataFieldInput)))

    reqObjectTypeInput.validate(Nil, default = Some(objectType)) shouldBe empty
  }

  it should "only override provided members" in {
    val reqObjectTypeInput = ValueTypeInput(Obj, members = Some(List(sub2DataFieldInput)))

    val valueType = reqObjectTypeInput.toValueType(default = Some(objectType))

    valueType shouldBe defined
    valueType.get.`type` shouldBe Obj
    valueType.get.asInstanceOf[ObjectType].members.map(_.name) should contain only sub2Name
    valueType.get.asInstanceOf[ObjectType].members.map(_.value.`type`) should contain only sub2DataField.value.`type`
  }

  "Number type input with object type default" should "not validate" in {
    numberTypeInput.validate(Nil, default = Some(objectType)) shouldBe defined
  }

  "Number type input with text type default" should "validate" in {
    numberTypeInput.validate(Nil, default = Some(textType)) shouldBe empty
  }

  it should "use number type" in {
    val valueType = numberTypeInput.toValueType(default = Some(textType))

    valueType shouldBe defined
    valueType.get.`type` shouldBe Number
  }

  "Undefined type input " should "not validate for Offering" in {
    undefinedTypeInput.validate(Nil, default = Some(textType), true) shouldBe defined
  }

  "Undefined type input " should "validate for OfferingQuery" in {
    undefinedTypeInput.validate(Nil, default = Some(textType), false) shouldBe empty
  }

  it should "use undefined type" in {
    val valueType = undefinedTypeInput.toValueType(default = None)

    valueType shouldBe defined
    valueType.get.`type` shouldBe Undefined
  }

}
