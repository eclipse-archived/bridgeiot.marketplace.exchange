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

import sangria.macros.derive.{GraphQLDescription, GraphQLField}
import microservice.entity.Id

import exchange.api.access._
import exchange.api.consumer._
import exchange.api.license._
import exchange.api.offering._
import exchange.api.offeringquery.OfferingQuery
import exchange.api.organization._
import exchange.api.price._
import exchange.api.provider._
import exchange.api.semantics._
import exchange.api.subscription.{SubscriptionId, Subscriptions}

@GraphQLDescription("BIG IoT Exchange queries")
trait ExchangeQueries {
  @GraphQLField
  @GraphQLDescription("List of all supported Currencies")
  def allCurrencies: List[Currency]

  @GraphQLField
  @GraphQLDescription("List of all supported PricingModels")
  def allPricingModels: List[PricingModel]

  @GraphQLField
  @GraphQLDescription("List of all supported Licenses")
  def allLicenses: List[License]

  @GraphQLField
  @GraphQLDescription("List of all supported EndpointTypes")
  def allEndpointTypes: List[EndpointType]

  @GraphQLField
  @GraphQLDescription("List of all supported AccessInterfaceTypes")
  def allAccessInterfaceTypes: List[AccessInterfaceType]

  @GraphQLField
  @GraphQLDescription("List of all OfferingCategories")
  def allOfferingCategories: OfferingCategory

  @GraphQLField
  @GraphQLDescription("List of all URIs for all OfferingCategories")
  def allOfferingCategoryUris: List[String]

  @GraphQLField
  @GraphQLDescription("Find OfferingCategory by rdfUri")
  def offeringCategory(rdfUri: String): Option[OfferingCategory]

  @GraphQLField
  @GraphQLDescription("Find default input DataField description")
  def inputDataField(typeUri: String, categoryUri: String): Option[DataField]

  @GraphQLField
  @GraphQLDescription("Find default output DataField description")
  def outputDataField(typeUri: String, categoryUri: String): Option[DataField]

  @GraphQLField
  @GraphQLDescription("List of all Data types")
  def allDataTypes: RdfAnnotations

  @GraphQLField
  @GraphQLDescription("List all Organizations")
  def allOrganizations: List[Organization]

  @GraphQLField
  @GraphQLDescription("Details about my Organization")
  def myOrganization: Option[Organization]

  @GraphQLField
  @GraphQLDescription("Find Organization by id")
  def organization(id: Id): Option[Organization]

  @GraphQLField
  @GraphQLDescription("Find Provider by id")
  def provider(id: Id): Option[Provider]

  @GraphQLField
  @GraphQLDescription("List all my Providers")
  def myProviders(offeringCategoryUri: Option[String]): List[Provider]

  @GraphQLField
  @GraphQLDescription("List all Offerings")
  def allOfferings(offeringCategoryUri: Option[String], onlyActive: Option[Boolean]): List[Offering]

  @GraphQLField
  @GraphQLDescription("List all my Offerings")
  def myOfferings(offeringCategoryUri: Option[String], onlyActive: Option[Boolean]): List[Offering]

  @GraphQLField
  @GraphQLDescription("List all Offerings registered by given Organization")
  def offeringsForOrganization(organizationId: Id, offeringCategoryUri: Option[String], onlyActive: Option[Boolean]): List[Offering]

  @GraphQLField
  @GraphQLDescription("Find Offering by id")
  def offering(id: Id): Option[Offering]

  @GraphQLField
  @GraphQLDescription("Find Offerings matching the OfferingQuery with given id")
  def matchingOfferings(queryId: Id): List[Offering]

  @GraphQLField
  @GraphQLDescription("Find Consumer by id")
  def consumer(id: Id): Option[Consumer]

  @GraphQLField
  @GraphQLDescription("List all my Consumers")
  def myConsumers: List[Consumer]

  @GraphQLField
  @GraphQLDescription("Find OfferingQuery by id")
  def offeringQuery(id: Id): Option[OfferingQuery]

  @GraphQLField
  @GraphQLDescription("List all Subscription for an Offering")
  def subscriptionsForOffering(id: OfferingId): Subscriptions

  @GraphQLField
  @GraphQLDescription("List all my Subscriptions")
  def mySubscriptions: Subscriptions

  @GraphQLField
  @GraphQLDescription("Find Subscription by id")
  def subscription(id: SubscriptionId): Option[OfferingSubscription]
}
