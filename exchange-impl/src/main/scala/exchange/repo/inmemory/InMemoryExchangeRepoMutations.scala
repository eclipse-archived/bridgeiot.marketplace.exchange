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

import monocle.function.all._
import microservice.entity._

import exchange.api.access.{AccessInterfaceType, BIGIOT_LIB, Endpoint, EndpointType}
import exchange.api.consumer._
import exchange.api.license.License
import exchange.api.offering._
import exchange.api.offeringquery._
import exchange.api.organization._
import exchange.api.price._
import exchange.api.provider._
import exchange.api.semantics._
import exchange.api.subscription.SubscriptionStatus._
import exchange.api.subscription._
import exchange.repo.{ExchangeRepoMutations, ExchangeSemanticRepo}

trait InMemoryExchangeRepoMutations extends ExchangeRepoMutations {
  this: InMemoryExchangeRepoQueries =>

  val isPersisting = false
  protected val semanticRepo: ExchangeSemanticRepo

  private def organizationLens(id: OrganizationId) = {
    val organizationIdx = exchange.organizations indexWhere entitiesMatch(id)
    if (organizationIdx >= 0)
      Some(Exchange.organizations composeOptional index(organizationIdx))
    else
      None
  }

  def organizationCreated(ev: OrganizationCreated) = {
    val organization = Organization(ev.id, ev.name)
    exchange = Exchange.organizations.modify(organization :: _)(exchange)
  }

  def organizationNameChanged(ev: OrganizationNameChanged) = organizationLens(ev.id) foreach { organization =>
    exchange = (organization ^|-> Organization.name).modify(_ => ev.name)(exchange)
  }

  private def providerLens(id: ProviderId) = for {
    org <- organizationForProvider(id)
    orgLens <- organizationLens(org.id)
    providerIdx = org.providers indexWhere entitiesMatch(id)
  } yield orgLens ^|-> Organization.providers composeOptional index(providerIdx)

  def providerCreated(ev: ProviderCreated) = organizationLens(ev.organizationId) foreach { organization =>
    val provider = Provider(ev.id, ev.name, None, ev.secret)
    exchange = (organization ^|-> Organization.providers).modify { providers =>
      provider :: (providers filterNot (_.id == ev.id))
    }(exchange)
  }

  def providerDeleted(ev: ProviderDeleted) = organizationLens(ev.organizationId) foreach { organization =>
    exchange = (organization ^|-> Organization.providers).modify(_.filterNot(_.id == ev.id))(exchange)
  }

  def providerNameChanged(ev: ProviderNameChanged) = providerLens(ev.id) foreach { provider =>
    exchange = (provider ^|-> Provider.name).modify(_ => ev.name)(exchange)
  }

  private def offeringLens(id: OfferingId) = for {
    offering <- offering(id)
    provider <- offering.provider
    provLens <- providerLens(provider.id)
    offeringIdx = provider.offerings indexWhere entitiesMatch(offering.id)
  } yield provLens ^|-> Provider.offerings composeOptional index(offeringIdx)

  private def createPrice(price: Price) =
    Price(price.pricingModel, price.money map { money => Money(money.amount, money.currency) })

  private def createDataField(categoryUri: String)(requested: DataFieldInput) = {
    val default = semanticRepo.inputDataField(requested.rdfUri, categoryUri)

    val name = requested.name.flatMap(name => if (!name.isEmpty) Some(name) else None).orElse(default.map(_.name)).getOrElse("")
    val rdfAnnotation = default.map(_.rdfAnnotation).getOrElse(RdfAnnotation(requested.rdfUri, ""))
    requested.value.flatMap(_.toValueType(default.map(_.value))).orElse(default.map(_.value)) map { value =>
      DataField(name, rdfAnnotation, value, requested.encodingType, requested.required)
    }
  }

  private def createRdfAnnotation(categoryUri: String)(typeUri: String) = semanticRepo.inputDataField(typeUri, categoryUri)

  def offeringCreated(ev: OfferingCreated) = providerLens(ev.providerId) foreach { provider =>
    val categoryUri = ev.rdfUri
    semanticRepo.offeringCategory(categoryUri) foreach { category =>
      val offering = Offering(ev.id, ev.name, None, ev.activation, category.rdfAnnotation, ev.rdfContext, ev.accessWhiteList, ev.endpoints,
        ev.outputs, ev.inputs, ev.extension1, ev.extension2, ev.extension3, ev.spatialExtent, ev.temporalExtent, ev.license, createPrice(ev.price))
      exchange = (provider ^|-> Provider.offerings).modify { offerings =>
        offering :: (offerings filterNot (_.id == ev.id))
      }(exchange)
    }
  }

  def offeringDeleted(ev: OfferingDeleted) = providerLens(ev.providerId) foreach { provider =>
    exchange = (provider ^|-> Provider.offerings).modify(_.filterNot(_.id == ev.id))(exchange)
  }

  private def updateOffering(offering: Offering) = {
    offeringLens(offering.id) foreach { offeringLens =>
      exchange = offeringLens.modify(_ => offering)(exchange)
    }
    consumerSubscriptionsForOffering(offering.id) foreach { case (consumer, _) =>
      consumerLens(consumer.id) foreach { consumerLens =>
        exchange = (consumerLens ^|-> Consumer.subscriptions).modify { subscriptions =>
          subscriptions map { subscription =>
            if (subscription.offering.id == offering.id) subscription.copy(offering = offering) else subscription
          }
        }(exchange)
      }
    }
    querySubscriptionsForOffering(offering.id) foreach { case (query, _) =>
      offeringQueryLens(query.id) foreach { offeringQueryLens =>
        exchange = (offeringQueryLens ^|-> OfferingQuery.subscriptions).modify { subscriptions =>
          subscriptions map { subscription =>
            if (subscription.offering.id == offering.id) subscription.copy(offering = offering) else subscription
          }
        }(exchange)
      }
    }
  }

  def offeringNameChanged(ev: OfferingNameChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(name = ev.name))
  }

  def offeringCategoryChanged(ev: OfferingCategoryChanged) = for {
    offering <- offering(ev.id)
    category <- semanticRepo.offeringCategory(ev.rdfUri)
  } yield updateOffering(offering.copy(rdfAnnotation = category.rdfAnnotation))

  def offeringAccessWhiteListChanged(ev: OfferingAccessWhiteListChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(accessWhiteList = ev.accessWhiteList))
  }

  def offeringEndpointsChanged(ev: OfferingEndpointsChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(endpoints = ev.endpoints))
  }

  def offeringInputDataChanged(ev: OfferingInputsChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(inputs = ev.inputs))
  }

  def offeringOutputDataChanged(ev: OfferingOutputsChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(outputs = ev.outputs))
  }

  def offeringSpatialExtentChanged(ev: OfferingSpatialExtentChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(spatialExtent = ev.spatialExtent))
  }

  def offeringTemporalExtentChanged(ev: OfferingTemporalExtentChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(temporalExtent = ev.temporalExtent))
  }

  def offeringLicenseChanged(ev: OfferingLicenseChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(license = ev.license))
  }

  def offeringPriceChanged(ev: OfferingPriceChanged) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(price = ev.price))
  }

  def offeringExtension1Changed(ev: OfferingExtension1Changed) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(extension1 = ev.value))
  }

  def offeringExtension2Changed(ev: OfferingExtension2Changed) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(extension2 = ev.value))
  }

  def offeringExtension3Changed(ev: OfferingExtension3Changed) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(extension3 = ev.value))
  }

  def offeringActivated(ev: OfferingActivated) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(activation = Activation(status = true, ev.expirationTime)))
  }

  def offeringDeactivated(ev: OfferingDeactivated) = offering(ev.id) foreach { offering =>
    updateOffering(offering.copy(activation = Activation(status = false)))
  }

  private def consumerLens(id: ConsumerId) = for {
    org <- organizationForConsumer(id)
    orgLens <- organizationLens(org.id)
    consumerIdx = org.consumers indexWhere entitiesMatch(id)
  } yield orgLens ^|-> Organization.consumers composeOptional index(consumerIdx)

  def consumerCreated(ev: ConsumerCreated) = organizationLens(ev.organizationId) foreach { organization =>
    exchange = (organization ^|-> Organization.consumers).modify { consumers =>
      Consumer(ev.id, ev.name) :: (consumers filterNot (_.id == ev.id))
    }(exchange)
  }

  def consumerDeleted(ev: ConsumerDeleted) = organizationLens(ev.organizationId) foreach { organization =>
    exchange = (organization ^|-> Organization.consumers).modify(_.filterNot(_.id == ev.id))(exchange)
  }

  def consumerNameChanged(ev: ConsumerNameChanged) = consumerLens(ev.id) foreach { consumer =>
    exchange = (consumer ^|-> Consumer.name).modify(_ => ev.name)(exchange)
  }

  private def offeringQueryLens(id: OfferingQueryId) = for {
    query <- offeringQuery(id)
    consumer <- query.consumer
    consLens <- consumerLens(consumer.id)
    queryIdx = consumer.queries indexWhere entitiesMatch(query.id)
  } yield consLens ^|-> Consumer.queries composeOptional index(queryIdx)

  def offeringQueryCreated(ev: OfferingQueryCreated) = consumerLens(ev.consumerId) foreach { consumer =>
    val rdfAnnotation = ev.rdfUri flatMap semanticRepo.offeringCategory map (_.rdfAnnotation)
    val category = ev.rdfUri getOrElse ""
    val query = OfferingQuery(ev.id, ev.name, None, ev.rdfContext, rdfAnnotation, ev.outputs, ev.inputs,
      ev.spatialExtent, ev.temporalExtent, ev.license, ev.price)
    exchange = (consumer ^|-> Consumer.queries).modify { queries =>
      query :: (queries filterNot (_.id == ev.id))
    }(exchange)
  }

  def offeringQueryDeleted(ev: OfferingQueryDeleted) = consumerLens(ev.consumerId) foreach { consumer =>
    exchange = (consumer ^|-> Consumer.queries).modify(_.filterNot(_.id == ev.id))(exchange)
  }

  def offeringQueryNameChanged(ev: OfferingQueryNameChanged) = offeringQueryLens(ev.id) foreach { query =>
    exchange = (query ^|-> OfferingQuery.name).modify(_ => ev.name)(exchange)
  }

  def offeringQueryCategoryChanged(ev: OfferingQueryCategoryChanged) = offeringQueryLens(ev.id) foreach { offeringQuery =>
    exchange = (offeringQuery ^|-> OfferingQuery.rdfAnnotation).modify(_ =>
      ev.rdfUri flatMap semanticRepo.offeringCategory map(_.rdfAnnotation))(exchange)
  }

  def offeringQueryInputDataChanged(ev: OfferingQueryInputsChanged) = for {
    offeringQuery <- offeringQueryLens(ev.id)
    rdfAnnotationOpt <- (offeringQuery ^|-> OfferingQuery.rdfAnnotation ).getOption(exchange)
    categoryUri = rdfAnnotationOpt map (_.uri) getOrElse ""
  } yield { exchange = (offeringQuery composeLens OfferingQuery.inputs).modify(_ => ev.inputs)(exchange)  }

  def offeringQueryOutputDataChanged(ev: OfferingQueryOutputsChanged) = for {
    offeringQuery <- offeringQueryLens(ev.id)
    rdfAnnotationOpt <- (offeringQuery ^|-> OfferingQuery.rdfAnnotation ).getOption(exchange)
    categoryUri = rdfAnnotationOpt map (_.uri) getOrElse ""
  } yield { exchange = (offeringQuery composeLens OfferingQuery.outputs).modify(_ => ev.outputs)(exchange) }

  def offeringQuerySpatialExtentChanged(ev: OfferingQuerySpatialExtentChanged) = offeringQueryLens(ev.id) foreach { offeringQuery =>
    exchange = (offeringQuery ^|-> OfferingQuery.spatialExtent).modify(_ => ev.spatialExtent)(exchange)
  }

  def offeringQueryTemporalExtentChanged(ev: OfferingQueryTemporalExtentChanged) = offeringQueryLens(ev.id) foreach { offeringQuery =>
    exchange = (offeringQuery ^|-> OfferingQuery.temporalExtent).modify(_ => ev.temporalExtent)(exchange)
  }

  def offeringQueryLicenseChanged(ev: OfferingQueryLicenseChanged) = offeringQueryLens(ev.id) foreach { query =>
    exchange = (query ^|-> OfferingQuery.license).modify(_ => ev.license)(exchange)
  }

  def offeringQueryPriceChanged(ev: OfferingQueryPriceChanged) = offeringQueryLens(ev.id) foreach { query =>
    exchange = (query ^|-> OfferingQuery.price).modify(_ => ev.price)(exchange)
  }

  private def deleteSubscriptionsForOffering[T <: OfferingSubscription](subscriptions: List[T], offeringId: Id) =
    subscriptions filterNot (_.offering.id.value == offeringId)

  private def changeSubscriptionStatusesForOffering[T <: OfferingSubscription](subscriptions: List[T], offeringId: Id, newStatus: SubscriptionStatus): List[T] =
    subscriptions map {
      case QueryToOfferingSubscription(id, offering, accessToken, query, _) if offering.id.value == offeringId =>
        QueryToOfferingSubscription(id, offering, accessToken, query, newStatus).asInstanceOf[T]
      case ConsumerToOfferingSubscription(id, offering, accessToken, consumer, _) if offering.id.value == offeringId =>
        ConsumerToOfferingSubscription(id, offering, accessToken, consumer, newStatus).asInstanceOf[T]
      case subscription =>
        subscription
    }

  def subscriptionCreated(ev: SubscriptionCreated) = {
    consumerLens(ev.subscriberId) foreach { consumer =>
      offering(ev.subscribableId) foreach { offering =>
        val subscription = ConsumerToOfferingSubscription(ev.id.value, offering, ev.accessToken)
        exchange = (consumer ^|-> Consumer.subscriptions).modify { subscriptions =>
          subscription :: deleteSubscriptionsForOffering(subscriptions, ev.subscribableId)
        }(exchange)
      }
    }
    offeringQueryLens(ev.subscriberId) foreach { query =>
      offering(ev.subscribableId) foreach { offering =>
        val subscription = QueryToOfferingSubscription(ev.id.value, offering, ev.accessToken)
        exchange = (query ^|-> OfferingQuery.subscriptions).modify { subscriptions =>
          subscription :: deleteSubscriptionsForOffering(subscriptions, ev.subscribableId)
        }(exchange)
      }
    }
  }

  def subscriptionDeleted(ev: SubscriptionDeleted) = {
    consumerLens(ev.subscriberId) foreach { consumer =>
      exchange = (consumer ^|-> Consumer.subscriptions).modify(subscriptions =>
        changeSubscriptionStatusesForOffering(subscriptions, ev.subscribableId, SubscriptionStatus.Inactive)
      )(exchange)
    }
    offeringQueryLens(ev.subscriberId) foreach { query =>
      exchange = (query ^|-> OfferingQuery.subscriptions).modify(subscriptions =>
        changeSubscriptionStatusesForOffering(subscriptions, ev.subscribableId, SubscriptionStatus.Inactive)
      )(exchange)
    }
  }

  // synchronize enums (ignore)
  def currenciesUpdated(currencies: Array[Currency]) = {}
  def pricingModelsUpdated(pricingModels: Array[PricingModel]) = {}
  def licensesUpdated(licenses: Array[License]) = {}
  def endpointTypesUpdated(endpointTypes: Array[EndpointType]) = {}
  def accessInterfaceTypesUpdated(accessInterfaceTypes: Array[AccessInterfaceType]) = {}

}
