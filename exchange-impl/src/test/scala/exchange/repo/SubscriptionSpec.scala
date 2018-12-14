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

import microservice.Meta

import exchange.ExchangeRepoSpec
import exchange.api.organization.OrganizationCreated
import exchange.api.provider.ProviderCreated
import exchange.api.consumer.ConsumerCreated
import exchange.api.offering.{Activation, Offering, OfferingCreated}
import exchange.api.offeringquery.{OfferingQueryCreated, QueryToOfferingSubscription, SubscribedQueryToOffering, UnsubscribedQueryFromOffering}
import exchange.api.subscription._

trait SubscriptionSpec extends ExchangeRepoSpec {

  def initRepos(repos: Seq[ExchangeRepoMutations]) = {
    repos.foreach(_.organizationCreated(OrganizationCreated(OrgId, OrgName, Meta())))
    repos.foreach(_.providerCreated(ProviderCreated(ProvId, OrgId, ProviderName, Secret, Meta())))
    repos.foreach(_.offeringCreated(OfferingCreated(OffId, ProvId, OfferingName, CategoryUri, None, NoOfferingAccessWhiteList, OfferingEndpoints,
      OutputDataFields, InputDataFields, None, None, None, SpatialExtent, TemporalExtent, DefaultLicense, DefaultPrice, Activation(status = true), Meta())))
    repos.foreach(_.consumerCreated(ConsumerCreated(ConsId, OrgId, ConsumerName, Secret, Meta())))
    repos.foreach(_.offeringQueryCreated(OfferingQueryCreated(QueryId, ConsId, OfferingQueryName, Some(CategoryUri), None,
      OutputDataFields, InputDataFields, SpatialExtent, TemporalExtent, DefaultLicenseOption, DefaultPriceOption, Meta())))
    repos.foreach(_.subscriptionCreated(SubscriptionCreated(SubscrId, QueryId, OffId, AccessToken, Meta())))
    repos.foreach(_.subscriptionCreated(SubscriptionCreated(OtherSubscrId, QueryId, OffId, AccessToken, Meta())))
  }

  "ExchangeRepo" should "contain Offering after creation" in { f =>
    f.queryRepo.offering(OffId).value should matchPattern {
      case Offering(OffId, OfferingName, _, _, _, None, _, _, _, _, _, _, _, _, _, DefaultLicense, DefaultPrice) =>
    }
  }

  it should "contain all Subscriptions for OfferingQuery after creation" in { f =>
    f.queryRepo.subscriptionsForQuery(QueryId).length shouldBe 2
  }

  it should "not contain Subscription after deletion" in { f =>
    f.mutationRepos.foreach(_.subscriptionDeleted(SubscriptionDeleted(SubscrId, QueryId, OffId, Meta())))
    f.queryRepo.subscriptionsForQuery(QueryId) shouldBe empty
  }

}
