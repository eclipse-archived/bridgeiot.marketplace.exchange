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
package exchange.repo

import microservice.entity.Id

import exchange.api.offering.OfferingCreated
import exchange.api.offeringquery.{OfferingQueryCreated, OfferingQueryId}
import exchange.api.semantics._

trait ExchangeSemanticRepo {
  def allOfferingCategories: OfferingCategory
  def allOfferingCategoryUris: List[String]
  def offeringCategory(rdfUri: String): Option[OfferingCategory]

  def offeringCategoryCreated(ev: OfferingCategoryCreated)
  def offeringCategoryDeprecated(ev: OfferingCategoryDeprecated)
  def offeringCategoryUndeprecated(ev: OfferingCategoryUndeprecated)
  def offeringCategoryNameChanged(ev: OfferingCategoryNameChanged)
  def offeringCategoryParentChanged(ev: OfferingCategoryParentChanged)

  def inputTypeAddedToOfferingCategory(ev: InputTypeAddedToOfferingCategory)
  def inputTypeDeprecatedForOfferingCategory(ev: InputTypeDeprecatedForOfferingCategory)
  def inputTypeUndeprecatedForOfferingCategory(ev: InputTypeUndeprecatedForOfferingCategory)
  def outputTypeAddedToOfferingCategory(ev: OutputTypeAddedToOfferingCategory)
  def outputTypeDeprecatedForOfferingCategory(ev: OutputTypeDeprecatedForOfferingCategory)
  def outputTypeUndeprecatedForOfferingCategory(ev: OutputTypeUndeprecatedForOfferingCategory)

  def allDataTypes: List[RdfAnnotation]

  /** Retrieve or create default category specific output DataField description
    * - If category exists, and the output type is already allowed, then its DataField description is returned
    * - If category exists, and the output type is not yet allowed, then it is added as allowed output DataField and returned
    * - If categoryUri is empty or category does not exist, None is returned
    * @param typeUri URI for output type
    * @param categoryUri URI for category where output type should be linked to
    * @return DataField description for that type (optional)
    */
  def outputDataField(typeUri: String, categoryUri: String): Option[DataField]

  /** Retrieve or create default category specific input DataField description
    * - If category exists, and the input type is already allowed, then its DataField description is returned
    * - If category exists, and the input type is not yet allowed, then it is added as allowed input DataField and returned
    * - If categoryUri is empty or category does not exist, None is returned
    * @param typeUri URI for output type
    * @param categoryUri URI for category where input type should be linked to
    * @return DataField description for that type (optional)
    */
  def inputDataField(typeUri: String, categoryUri: String): Option[DataField]

  def isOfferingConsistent(ev: OfferingCreated): Boolean
  def isOfferingQueryConsistent(ev: OfferingQueryCreated): Boolean

  def matchingOfferingIds(queryId: OfferingQueryId): List[Id]
}
