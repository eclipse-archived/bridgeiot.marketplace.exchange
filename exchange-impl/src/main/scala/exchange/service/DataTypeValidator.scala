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
package exchange.service

import exchange.api.semantics.{DataField, DataFieldInput, RdfAnnotation}
import exchange.repo.ExchangeSemanticRepo

trait DataTypeValidator {
  val semanticRepo: ExchangeSemanticRepo

  def toDataField(categoryUri: String)(requested: DataFieldInput) = {
    val default = semanticRepo.inputDataField(requested.rdfUri, categoryUri)
    val name = requested.name.flatMap(name => if (!name.isEmpty) Some(name) else None).orElse(default.map(_.name)).getOrElse("")
    val rdfAnnotation = default.map(_.rdfAnnotation).getOrElse(RdfAnnotation(requested.rdfUri, ""))
    requested.value.flatMap(_.toValueType(default.map(_.value))).orElse(default.map(_.value)) map { value =>
      DataField(name, rdfAnnotation, value, requested.encodingType, requested.required)
    }
  }

  def invalidDataTypes(categoryUri: String, kind: String, forOffering: Boolean, fields: List[DataFieldInput]) =
    fields flatMap validateDataFieldInput(categoryUri, kind, semanticRepo.allOfferingCategoryUris, forOffering)

  def validateDataFieldInput(categoryUri: String, kind: String, categoryUris: List[String], forOffering: Boolean)(dataFieldInput: DataFieldInput) =
    if (categoryUris.contains(dataFieldInput.rdfUri)) Some(s"${dataFieldInput.rdfUri} is already used as category URI")
    else {
      val default = kind match {
        case "input" => semanticRepo.inputDataField(dataFieldInput.rdfUri, categoryUri)
        case _ => semanticRepo.outputDataField(dataFieldInput.rdfUri, categoryUri)
      }
      dataFieldInput.validate(Nil, default, forOffering).map(kind + " " + _)
    }

}
