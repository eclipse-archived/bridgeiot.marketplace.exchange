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

import exchange.api.semantics._

trait SemanticsFixtures {
  val numberUri = "schema:number"
  val textUri = "schema:text"
  val arrayUri = "schema:array"
  val objectUri = "schema:object"
  val numberAnnotation = RdfAnnotation(numberUri, Number)
  val textAnnotation = RdfAnnotation(textUri, Text)
  val arrayAnnotation = RdfAnnotation(arrayUri, Array)
  val objectAnnotation = RdfAnnotation(objectUri, Obj)

  val name = "nName"
  val sub1Name = "sub1"
  val sub2Name = "sub2"
  val encodingType = "encodingType"
  val required = true

  val numberTypeInput = ValueTypeInput(Number)
  val textTypeInput = ValueTypeInput(Text)
  val arrayTypeInput = ValueTypeInput(Array, element = Some(textTypeInput))
  val undefinedTypeInput = ValueTypeInput(Undefined)

  val numberDataFieldInput = DataFieldInput(Some(name), numberUri, Some(numberTypeInput))
  val sub1DataFieldInput = DataFieldInput(Some(sub1Name), numberUri, Some(numberTypeInput))
  val sub2DataFieldInput = DataFieldInput(Some(sub2Name), arrayUri, Some(arrayTypeInput))

  val objectTypeInput = ValueTypeInput(Obj, members = Some(List(sub1DataFieldInput, sub2DataFieldInput)))

  val numberType = NumberType()
  val textType = TextType()
  val arrayType = ArrayType(element = textType)

  val numberDataField = Some(DataField(name, numberAnnotation, numberType))
  val sub1DataField = DataField(sub1Name, numberAnnotation, numberType)
  val sub2DataField = DataField(sub2Name, arrayAnnotation, arrayType)

  val objectType = ObjectType(members = List(sub1DataField, sub2DataField))

}
