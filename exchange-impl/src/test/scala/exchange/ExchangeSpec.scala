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
package exchange

import microservice.entity.Sep

import exchange.api.access._
import exchange.api.consumer.ConsumerId
import exchange.api.extent
import exchange.api.license._
import exchange.api.offering.OfferingId
import exchange.api.offeringquery.OfferingQueryId
import exchange.api.organization.OrganizationId
import exchange.api.price._
import exchange.api.provider.ProviderId
import exchange.api.semantics.{DataField, DataFieldInput, NumberType, RdfAnnotation, ValueTypeInput}
import exchange.api.subscription.SubscriptionId

trait ExchangeSpec {

  val OrgName = "Organization"
  val OtherOrgName = "OtherOrganization"
  val ChangedOrgName = "Organization2"
  val OrgId = OrganizationId(OrgName)
  val OtherOrgId = OtherOrgName

  val ProviderName = "Provider"
  val OtherProviderName = "OtherProvider"
  val ChangedProviderName = "Provider2"
  val ProvId = ProviderId(OrgId.value + Sep + ProviderName)
  val OtherProviderId = ProviderId(OrgId.value + Sep + OtherProviderName)

  val ConsumerName = "Consumer"
  val OtherConsumerName = "OtherConsumer"
  val ChangedConsumerName = "Consumer2"
  val ConsId = ConsumerId(OrgId.value + Sep + ConsumerName)
  val OtherConsId = ConsumerId(OrgId.value + Sep + OtherConsumerName)

  val OfferingName = "Offering"
  val OtherOfferingName = "OtherOffering"
  val ChangedOfferingName = "Offering2"
  val OffId = OfferingId(ProvId.value + Sep + OfferingName)
  val OtherOfferingId = OfferingId(ProvId.value + Sep + OtherOfferingName)

  val CategoryUri = "urn:big-iot:MobilityCategory"
  val CategoryLabel = "Mobility"
  val OtherCategoryUri = "urn:big-iot:SmartHomeCategory"
  val OtherCategoryLabel = "Smart Home"
  val ChangedCategoryUri = OtherCategoryUri
  val Category = RdfAnnotation(CategoryUri, CategoryLabel)
  val OtherCategory = RdfAnnotation(OtherCategoryUri, OtherCategoryLabel)

  val SpatialExtent = Some(extent.SpatialExtent("City"))
  val ChangedSpatialExtent = Some(extent.SpatialExtent("City2"))
  val TemporalExtent = None
  val ChangedTemporalExtent = Some(extent.TemporalExtent())

  val OfferingQueryName = "OfferingQuery"
  val OtherOfferingQueryName = "OtherOfferingQuery"
  val ChangedOfferingQueryName = "OfferingQuery2"
  val QueryId = OfferingQueryId(ConsId.value + Sep + OfferingQueryName)
  val OtherQueryId = OfferingQueryId(ConsId.value + Sep + OtherOfferingQueryName)

  val NoOfferingAccessWhiteList = Nil

  val OfferingEndpoint = Endpoint(HTTP_GET, "uri", BIGIOT_LIB)
  val OfferingEndpoints = List(OfferingEndpoint)
  val ChangedOfferingEndpoint = EndpointInput(HTTP_GET, "uri2", None)
  val ChangedOfferingEndpoints = List(ChangedOfferingEndpoint)

  val InputDataField = DataField("infield", Category, NumberType())
  val InputDataFields = List(InputDataField)
  val ChangedInputDataField = DataField("infield", OtherCategory, NumberType())
  val ChangedInputDataFields = List(ChangedInputDataField)

  val OutputDataField = DataField("outfield", Category, NumberType())
  val OutputDataFields = List(OutputDataField)
  val ChangedOutputDataField = DataField("outfield", OtherCategory, NumberType())
  val ChangedOutputDataFields = List(ChangedOutputDataField)

  val Free = Price(FREE, None)
  val EUR5 = Price(PER_MONTH, Some(Money(5, EUR)))

  val DefaultPrice = Free
  val ChangedPrice = EUR5

  val PriceFree = Some(Free)
  val PriceEUR5 = Some(EUR5)

  val DefaultPriceOption = PriceFree
  val ChangedPriceOption = PriceEUR5

  val DefaultLicense = OPEN_DATA_LICENSE
  val ChangedLicense = CREATIVE_COMMONS

  val DefaultLicenseOption = Some(DefaultLicense)
  val ChangedLicenseOption = Some(ChangedLicense)

  val SubscrId = SubscriptionId("Subscription")

  val OtherSubscrId = SubscriptionId("OtherSubscription")

  val Secret = "Secret"
  val AccessToken = "AccessToken"

}
