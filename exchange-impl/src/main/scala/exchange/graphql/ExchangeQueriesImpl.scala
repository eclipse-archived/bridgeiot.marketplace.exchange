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
package exchange.graphql

import io.funcqrs.AggregateId
import microservice._
import microservice.entity.Id

import exchange.api.ExchangeQueries
import exchange.api.access._
import exchange.api.consumer.ConsumerId
import exchange.api.license._
import exchange.api.offering._
import exchange.api.offeringquery.OfferingQueryId
import exchange.api.organization._
import exchange.api.price._
import exchange.api.provider.ProviderId
import exchange.api.semantics._
import exchange.api.subscription.{SubscriptionId, Subscriptions}
import exchange.repo.{ExchangeRepoQueries, ExchangeSemanticRepo}

class ExchangeQueriesImpl(requesterId: Option[String], requesterOrgId: Option[OrganizationId],
                          queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo) extends ExchangeQueries {
  def allCurrencies = currencies

  def allPricingModels = pricingModels

  def allLicenses = licenses

  def allEndpointTypes = endpointTypes

  def allAccessInterfaceTypes = accessInterfaceTypes

  def allOfferingCategories = semanticRepo.allOfferingCategories

  def allOfferingCategoryUris = semanticRepo.allOfferingCategoryUris

  def offeringCategory(rdfUri: String) = semanticRepo.offeringCategory(rdfUri)

  def inputDataField(typeUri: String, categoryUri: String) = semanticRepo.inputDataField(typeUri, categoryUri)

  def outputDataField(typeUri: String, categoryUri: String) = semanticRepo.outputDataField(typeUri, categoryUri)

  def allDataTypes = RdfAnnotations(semanticRepo.allDataTypes)

  def allOrganizations = queryRepo.allOrganizations

  def myOrganization = my(queryRepo.organization, None)

  def organization(id: Id) = queryRepo.organization(id)

  def provider(id: Id) = authorize(queryRepo.provider, ProviderId(id), None)

  def myProviders(offeringCategoryUri: Option[String]) =
    my(queryRepo.providersForOrganization(offeringCategoryUri), Nil)

  def allOfferings(offeringCategoryUri: Option[String], onlyActive: Option[Boolean]) =
    queryRepo.allOfferings(offeringCategoryUri, onlyActive) filter accessControl

  def myOfferings(offeringCategoryUri: Option[String], onlyActive: Option[Boolean]) =
    my(queryRepo.offeringsForOrganization(offeringCategoryUri, onlyActive), Nil)

  def offeringsForOrganization(organizationId: Id, offeringCategoryUri: Option[String], onlyActive: Option[Boolean]) =
    queryRepo.offeringsForOrganization(offeringCategoryUri, onlyActive)(organizationId) filter accessControl

  def offering(id: Id) = queryRepo.offering(id) filter accessControl

  def matchingOfferings(queryId: Id) =
    authorize(semanticRepo.matchingOfferingIds, OfferingQueryId(queryId), Nil).flatMap(offering(_).toList) filter accessControl

  def consumer(id: Id) = authorize(queryRepo.consumer, ConsumerId(id), None)

  def myConsumers = my(queryRepo.consumersForOrganization, Nil)

  def offeringQuery(id: Id) = authorize(queryRepo.offeringQuery, OfferingQueryId(id), None)

  def subscriptionsForOffering(id: OfferingId) = authorize(queryRepo.subscriptionsForOffering, id, Subscriptions())

  def mySubscriptions = my(queryRepo.subscriptionsForOrganization, Subscriptions())

  def subscription(id: SubscriptionId) = authorize(queryRepo.subscription, id, None)

  // helper
  private def authorize[I <: AggregateId, T](f: I => T, id: I, empty: T) =
//    if (hasWrongOrganization(requesterId, requesterOrgId.map(_.value), id))
//      empty
//    else
      f(id)

  private def my[T](f: OrganizationId => T, empty: T) = requesterOrgId.map(f).getOrElse(empty)

  private def accessControl(offering: Offering) =
    offering.accessWhiteList.isEmpty || (requesterOrgId.isDefined && (requesterOrgId.get.value == entity.organizationId(offering.id) ||
      offering.accessWhiteList.contains(requesterOrgId.get.value)))

}
