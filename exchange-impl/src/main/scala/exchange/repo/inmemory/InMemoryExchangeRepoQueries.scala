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
package exchange.repo.inmemory

import monocle.macros.Lenses
import microservice.entity.entitiesMatch

import exchange.api.consumer._
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization._
import exchange.api.provider._
import exchange.api.semantics.{OfferingCategory, RdfAnnotation, RootOfferingCategoryUri}
import exchange.api.subscription.{SubscriptionId, Subscriptions}
import exchange.repo.{ExchangeRepoQueries, ExchangeSemanticRepo}

@Lenses
case class Exchange(organizations: List[Organization])

trait InMemoryExchangeRepoQueries extends ExchangeRepoQueries {

  protected var exchange: Exchange
  protected val semanticRepo: ExchangeSemanticRepo

  def show = ("Exchange:" +: exchange.organizations.map(_.show("  "))).mkString("\n")

  def allOrganizations = exchange.organizations map fillOrganization

  def organization(id: OrganizationId) = exchange.organizations find entitiesMatch(id) map fillOrganization

  private def fillOrganization(organization: Organization) =
    organization.copy(providers = providersFor(organization), consumers = consumersFor(organization))

  protected def organizationForProvider(providerId: ProviderId) =
    exchange.organizations find { org => org.providers exists entitiesMatch(providerId) }

  protected def organizationForConsumer(consumerId: ConsumerId) =
    exchange.organizations find { org => org.consumers exists entitiesMatch(consumerId) }

  def providersFor(organization: Organization) =
    organization.providers map { provider =>
      val providerWithOrganization = provider.copy(organization = Some(organization))
      providerWithOrganization.copy(offerings = offeringsFor(providerWithOrganization))
    }

  def provider(id: ProviderId) = (for {
    organization <- exchange.organizations
    provider <- providersFor(organization) if entitiesMatch(id)(provider)
  } yield provider).headOption

  def providersForOrganization(categoryUriOpt: Option[String])(organizationId: OrganizationId) = (for {
    offeringCategoryUri <- categoryUriOpt orElse Some(RootOfferingCategoryUri)
    category <- semanticRepo.offeringCategory(offeringCategoryUri)
    organization <- organization(organizationId)
  } yield for {
    provider <- providersFor(organization)
    if offeringCategoryUri.contains("all") || (provider.offerings exists { offering => isChild(category, offering.rdfAnnotation) })
  } yield provider.copy(organization = Some(organization))) getOrElse Nil

  private def offeringsFor(provider: Provider) = provider.offerings map (_.copy(provider = Some(provider)))

  def offering(id: OfferingId) = (for {
    organization <- exchange.organizations
    provider <- providersFor(organization)
    offering <- offeringsFor(provider) if entitiesMatch(id)(offering)
  } yield offering).headOption

  private def isChild(parent: OfferingCategory, child: RdfAnnotation): Boolean =
    (parent.rdfAnnotation.uri == child.uri) || (parent.subCategories exists { subCategory => isChild(subCategory, child) })

  def allOfferings(categoryUriOpt: Option[String], onlyActiveOpt: Option[Boolean]) = (for {
    offeringCategoryUri <- categoryUriOpt orElse Some(RootOfferingCategoryUri)
    category <- semanticRepo.offeringCategory(offeringCategoryUri)
    showInactive = onlyActiveOpt.isEmpty || !onlyActiveOpt.get
  } yield for {
    organization <- exchange.organizations
    provider <- providersFor(organization)
    offering <- offeringsFor(provider) if isChild(category, offering.rdfAnnotation) && (showInactive || offering.activation.isActive)
  } yield offering) getOrElse Nil

  def offeringsForOrganization(categoryUriOpt: Option[String], onlyActiveOpt: Option[Boolean])(organizationId: OrganizationId) = (for {
    offeringCategoryUri <- categoryUriOpt orElse Some(RootOfferingCategoryUri)
    category <- semanticRepo.offeringCategory(offeringCategoryUri)
    organization <- organization(organizationId)
    showInactive = onlyActiveOpt.isEmpty || !onlyActiveOpt.get
  } yield for {
    provider <- providersFor(organization)
    offering <- offeringsFor(provider) if isChild(category, offering.rdfAnnotation) && (showInactive || offering.activation.isActive)
  } yield offering) getOrElse Nil

  private def consumersFor(organization: Organization) = organization.consumers map { consumer =>
    val consumerWithOrganization = consumer.copy(organization = Some(organization))
    consumerWithOrganization.copy(queries = queriesFor(consumerWithOrganization))
  }

  def consumersForOrganization(organizationId: OrganizationId) = organization(organizationId).toList flatMap consumersFor

  def consumer(id: ConsumerId) = (for {
    organization <- exchange.organizations
    consumer <- consumersFor(organization) if entitiesMatch(id)(consumer)
  } yield consumer).headOption

  private def queriesFor(consumer: Consumer) = consumer.queries map (_.copy(consumer = Some(consumer)))

  def offeringQueriesForConsumer(consumerId: ConsumerId) = consumer(consumerId).toList flatMap queriesFor

  def offeringQuery(id: OfferingQueryId) = (for {
    organization <- exchange.organizations
    consumer <- consumersFor(organization)
    query <- queriesFor(consumer) if entitiesMatch(id)(query)
  } yield query).headOption

  private def subscriptionsFor(consumer: Consumer) = consumer.subscriptions map (_.copy(consumer = Some(consumer)))

  def subscriptionsForConsumer(consumerId: ConsumerId) = consumer(consumerId).toList flatMap subscriptionsFor

  private def subscriptionsFor(query: OfferingQuery) = query.subscriptions map (_.copy(query = Some(query)))

  def subscriptionsForQuery(queryId: OfferingQueryId) = offeringQuery(queryId).toList flatMap subscriptionsFor

  def consumerSubscriptionsForOffering(offeringId: OfferingId) = {
    for {
      organization <- exchange.organizations
      consumer <- consumersFor(organization)
      consumerSubscription <- subscriptionsFor(consumer) if consumerSubscription.offering.id == offeringId
    } yield (consumer, consumerSubscription)
  }

  def querySubscriptionsForOffering(offeringId: OfferingId) = {
    for {
      organization <- exchange.organizations
      consumer <- consumersFor(organization)
      query <- queriesFor(consumer)
      querySubscription <- subscriptionsFor(query) if querySubscription.offering.id == offeringId
    } yield (query, querySubscription)
  }

  def subscriptionsForOffering(offeringId: OfferingId) = {
    val consumerSubscriptions = consumerSubscriptionsForOffering(offeringId) map (_._2)
    val querySubscriptions = querySubscriptionsForOffering(offeringId) map (_._2)
    Subscriptions(consumerSubscriptions, querySubscriptions)
  }

  def subscriptionsForOrganization(organizationId: OrganizationId) = {
    exchange.organizations.find(_.id == organizationId) map { organization =>
      val consumerSubscriptions = for {
        consumer <- consumersFor(organization)
        consumerSubscription <- subscriptionsFor(consumer)
      } yield consumerSubscription
      val querySubscriptions = for {
        consumer <- consumersFor(organization)
        query <- queriesFor(consumer)
        querySubscription <- subscriptionsFor(query)
      } yield querySubscription
      Subscriptions(consumerSubscriptions, querySubscriptions)
    } getOrElse Subscriptions()
  }

  def subscription(id: SubscriptionId) = (for {
    organization <- exchange.organizations
    consumer <- consumersFor(organization)
    subscriptionsForQuery = queriesFor(consumer) flatMap subscriptionsFor
    subscription <- subscriptionsFor(consumer) ++ subscriptionsForQuery if entitiesMatch(id)(subscription)
  } yield subscription).headOption

}
