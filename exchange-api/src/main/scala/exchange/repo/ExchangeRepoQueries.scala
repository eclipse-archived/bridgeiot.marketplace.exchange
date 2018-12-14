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

import exchange.api.consumer._
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization._
import exchange.api.provider._
import exchange.api.subscription._

trait ExchangeRepoQueries {
  def show: String

  def allOrganizations: List[Organization]
  def organization(id: OrganizationId): Option[Organization]

  def provider(id: ProviderId): Option[Provider]
  def providersForOrganization(offeringCategoryUri: Option[String] = None)(organizationId: OrganizationId): List[Provider]

  def offering(id: OfferingId): Option[Offering]
  def allOfferings(offeringCategoryUri: Option[String] = None, onlyActive: Option[Boolean] = None): List[Offering]
  def offeringsForOrganization(offeringCategoryUri: Option[String] = None, onlyActive: Option[Boolean] = None)(organizationId: OrganizationId): List[Offering]

  def consumersForOrganization(organizationId: OrganizationId): List[Consumer]
  def consumer(id: ConsumerId): Option[Consumer]

  def offeringQueriesForConsumer(consumerId: ConsumerId): List[OfferingQuery]
  def offeringQuery(id: OfferingQueryId): Option[OfferingQuery]

  def subscriptionsForConsumer(consumerId: ConsumerId): List[ConsumerToOfferingSubscription]
  def subscriptionsForQuery(queryId: OfferingQueryId): List[QueryToOfferingSubscription]
  def subscriptionsForOffering(offeringId: OfferingId): Subscriptions
  def subscriptionsForOrganization(organizationId: OrganizationId): Subscriptions
  def subscription(id: SubscriptionId): Option[OfferingSubscription]
}




