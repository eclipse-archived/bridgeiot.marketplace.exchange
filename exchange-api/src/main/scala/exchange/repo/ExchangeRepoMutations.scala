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

import exchange.api.access.{AccessInterfaceType, EndpointType}
import exchange.api.consumer.{ConsumerCreated, ConsumerDeleted, ConsumerNameChanged}
import exchange.api.license.License
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization.{OrganizationCreated, OrganizationNameChanged}
import exchange.api.price.{Currency, PricingModel}
import exchange.api.provider.{ProviderCreated, ProviderDeleted, ProviderNameChanged}
import exchange.api.subscription.{SubscriptionCreated, SubscriptionDeleted}

trait ExchangeRepoMutations {
  def isPersisting: Boolean

  def organizationCreated(ev: OrganizationCreated)
  def organizationNameChanged(ev: OrganizationNameChanged)

  def providerCreated(ev: ProviderCreated)
  def providerDeleted(ev: ProviderDeleted)
  def providerNameChanged(ev: ProviderNameChanged)

  def offeringCreated(ev: OfferingCreated)
  def offeringDeleted(ev: OfferingDeleted)
  def offeringNameChanged(ev: OfferingNameChanged)
  def offeringCategoryChanged(ev: OfferingCategoryChanged)
  def offeringAccessWhiteListChanged(ev: OfferingAccessWhiteListChanged)
  def offeringEndpointsChanged(ev: OfferingEndpointsChanged)
  def offeringInputDataChanged(ev: OfferingInputsChanged)
  def offeringOutputDataChanged(ev: OfferingOutputsChanged)
  def offeringSpatialExtentChanged(ev: OfferingSpatialExtentChanged)
  def offeringTemporalExtentChanged(ev: OfferingTemporalExtentChanged)
  def offeringLicenseChanged(ev: OfferingLicenseChanged)
  def offeringPriceChanged(ev: OfferingPriceChanged)
  def offeringExtension1Changed(ev: OfferingExtension1Changed)
  def offeringExtension2Changed(ev: OfferingExtension2Changed)
  def offeringExtension3Changed(ev: OfferingExtension3Changed)
  def offeringActivated(ev: OfferingActivated)
  def offeringDeactivated(ev: OfferingDeactivated)

  def consumerCreated(ev: ConsumerCreated)
  def consumerDeleted(ev: ConsumerDeleted)
  def consumerNameChanged(ev: ConsumerNameChanged)

  def offeringQueryCreated(ev: OfferingQueryCreated)
  def offeringQueryDeleted(ev: OfferingQueryDeleted)
  def offeringQueryNameChanged(ev: OfferingQueryNameChanged)
  def offeringQueryCategoryChanged(ev: OfferingQueryCategoryChanged)
  def offeringQueryInputDataChanged(ev: OfferingQueryInputsChanged)
  def offeringQueryOutputDataChanged(ev: OfferingQueryOutputsChanged)
  def offeringQuerySpatialExtentChanged(ev: OfferingQuerySpatialExtentChanged)
  def offeringQueryTemporalExtentChanged(ev: OfferingQueryTemporalExtentChanged)
  def offeringQueryLicenseChanged(ev: OfferingQueryLicenseChanged)
  def offeringQueryPriceChanged(ev: OfferingQueryPriceChanged)

  def subscriptionCreated(ev: SubscriptionCreated)
  def subscriptionDeleted(ev: SubscriptionDeleted)

  // synchronize enums
  def currenciesUpdated(currencies: Array[Currency])
  def pricingModelsUpdated(pricingModels: Array[PricingModel])
  def licensesUpdated(licenseTypes: Array[License])
  def endpointTypesUpdated(endpointTypes: Array[EndpointType])
  def accessInterfaceTypesUpdated(accessInterfaceTypes: Array[AccessInterfaceType])
}
