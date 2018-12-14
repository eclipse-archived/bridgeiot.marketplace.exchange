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
import exchange.api.consumer.ConsumerCreated
import exchange.api.offering.{Activation, OfferingCreated}
import exchange.api.offeringquery.OfferingQueryCreated
import exchange.api.organization.OrganizationCreated
import exchange.api.provider.ProviderCreated

trait MatchingSpec extends ExchangeRepoSpec {

  def initRepos(repos: Seq[ExchangeRepoMutations]) = {
    repos.foreach(_.organizationCreated(OrganizationCreated(OrgId, OrgName, Meta())))
    repos.foreach(_.providerCreated(ProviderCreated(ProvId, OrgId, ProviderName, Secret, Meta())))
    repos.foreach(_.offeringCreated(OfferingCreated(OffId, ProvId, OfferingName, CategoryUri, None, NoOfferingAccessWhiteList, OfferingEndpoints,
      OutputDataFields, InputDataFields, None, None, None, SpatialExtent, TemporalExtent, DefaultLicense, DefaultPrice, Activation(status = true), Meta())))
    repos.foreach(_.consumerCreated(ConsumerCreated(ConsId, OrgId, ConsumerName, Secret, Meta())))
    repos.foreach(_.offeringQueryCreated(OfferingQueryCreated(QueryId, ConsId, OfferingQueryName, Some(CategoryUri), None,
      OutputDataFields, InputDataFields, SpatialExtent, TemporalExtent, DefaultLicenseOption, DefaultPriceOption, Meta())))
  }

  "SemanticRepo" should "contain one Offering matching the OfferingQuery" in { f =>
    f.semanticRepo.matchingOfferingIds(QueryId).length shouldBe 1
  }

}
