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

import io.circe.generic.auto._
import microservice._

import exchange.api.consumer.{ConsumerEvent, ConsumerToOfferingSubscription, SubscribedConsumerToOffering, UnsubscribedConsumerFromOffering}
import exchange.api.offering.{OfferingDeleted, OfferingEvent, OfferingId}
import exchange.api.offeringquery._
import exchange.api.subscription.{CreateSubscription, DeleteSubscription, SubscriptionCommand, SubscriptionId}
import exchange.api.subscription.SubscriptionStatus._
import exchange.api.{consumer, offering, offeringquery, subscription}
import exchange.model.Subscription
import exchange.repo.ExchangeRepoQueries
import exchange.server.Exchange.{materializer, system}

case class  SubscriptionService(queryRepo: ExchangeRepoQueries) extends AkkaServiceBackend(subscription.serviceName, Subscription.behavior) {
  override def eventAdapter = Some(EventAdapterStage(subscription.serviceName,
    List(
    eventTopic[OfferingEvent](offering.serviceName),
    eventTopic[ConsumerEvent](consumer.serviceName),
    eventTopic[OfferingQueryEvent](offeringquery.serviceName)
  ), cmdTopic[SubscriptionCommand](subscription.serviceName), {

    case OfferingDeleted(offeringId, _, meta) =>
      val subscriptions = queryRepo.subscriptionsForOffering(offeringId)
      subscriptions.consumerSubscriptions ++ subscriptions.querySubscriptions map {
        case ConsumerToOfferingSubscription(subscriptionId, offering, _, consumer, status) if status == Active || status == Idle =>
          DeleteSubscription(subscriptionId, consumer.map(_.id.value).getOrElse(""), offering.id, meta)
        case QueryToOfferingSubscription(subscriptionId, offering, _, query, status) if status == Active || status == Idle =>
          DeleteSubscription(subscriptionId, query.map(_.id.value).getOrElse(""), offering.id, meta)
      }

    case OfferingQueryDeleted(queryId, _, meta) => queryRepo.subscriptionsForQuery(queryId) map { subscription =>
      DeleteSubscription(subscription.id, queryId, subscription.offering.id, meta)
    }

    case SubscribedConsumerToOffering(consumerId, offeringId, meta) => providerSecret(offeringId).toList.map { secret =>
      CreateSubscription(SubscriptionId(consumerId, offeringId), consumerId, offeringId, secret, meta)
    }

    case UnsubscribedConsumerFromOffering(consumerId, offeringId, meta) =>
      DeleteSubscription(SubscriptionId(consumerId, offeringId), consumerId, offeringId, meta)


    case SubscribedQueryToOffering(queryId, offeringId, meta) => providerSecret(offeringId).toList.map { secret =>
      CreateSubscription(SubscriptionId(queryId, offeringId), queryId, offeringId, secret, meta)
    }

    case UnsubscribedQueryFromOffering(queryId, offeringId, meta) =>
      DeleteSubscription(SubscriptionId(queryId, offeringId), queryId, offeringId, meta)
  }))

  def providerSecret(offeringId: OfferingId) = for {
    offering <- queryRepo.offering(offeringId)
    provider <- offering.provider
  } yield provider.secret

}