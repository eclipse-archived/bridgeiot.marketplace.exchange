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
import exchange.api.offeringquery._
import exchange.api.organization.OrganizationCreated
import exchange.api.semantics.RdfAnnotation

trait OfferingQuerySpec extends ExchangeRepoSpec {

  def initRepos(repos: Seq[ExchangeRepoMutations]) = {
    repos.foreach(_.organizationCreated(OrganizationCreated(OrgId, OrgName, Meta())))
    repos.foreach(_.consumerCreated(ConsumerCreated(ConsId, OrgId, ConsumerName, Secret, Meta())))
    repos.foreach(_.offeringQueryCreated(OfferingQueryCreated(QueryId, ConsId, OfferingQueryName, Some(CategoryUri), None,
      OutputDataFields, InputDataFields, SpatialExtent, TemporalExtent, DefaultLicenseOption, DefaultPriceOption, Meta())))
    repos.foreach(_.offeringQueryCreated(OfferingQueryCreated(OtherQueryId, ConsId, OtherOfferingQueryName, Some(OtherCategoryUri),
      None, OutputDataFields, InputDataFields, SpatialExtent, TemporalExtent, DefaultLicenseOption, DefaultPriceOption, Meta())))
  }

  "ExchangeRepo" should "contain OfferingQuery after creation" in { f =>
    f.queryRepo.offeringQuery(QueryId).value should matchPattern {
      case OfferingQuery(QueryId, OfferingQueryName, _, None, _, _, _, _, _, DefaultLicenseOption, DefaultPriceOption, _) =>
    }
  }

  it should "contain all OfferingQueries for some Consumer after creation" in { f =>
    f.queryRepo.offeringQueriesForConsumer(ConsId).length shouldBe 2
  }

  it should "allow changing OfferingQuery name" in { f =>
    f.mutationRepos.foreach(_.offeringQueryNameChanged(OfferingQueryNameChanged(QueryId, ChangedOfferingQueryName, Meta())))
    f.queryRepo.offeringQuery(QueryId).value should matchPattern {
      case OfferingQuery(QueryId, ChangedOfferingQueryName, _, None, _, _, _, _, _, DefaultLicenseOption, DefaultPriceOption, _) =>
    }
  }

  it should "allow changing OfferingQuery type" in { f =>
    f.mutationRepos.foreach(_.offeringQueryCategoryChanged(OfferingQueryCategoryChanged(QueryId, Some(ChangedCategoryUri), Meta())))
    f.queryRepo.offeringQuery(QueryId).value should matchPattern {
      case OfferingQuery(QueryId, OfferingQueryName, _, None, Some(RdfAnnotation(ChangedCategoryUri, _, _, _)), _, _, _, _, DefaultLicenseOption, DefaultPriceOption, _) =>
    }
  }

  // TODO: add tests for changing inputs, outputs

  it should "allow changing OfferingQuery license" in { f =>
    f.mutationRepos.foreach(_.offeringQueryLicenseChanged(OfferingQueryLicenseChanged(QueryId, ChangedLicenseOption, Meta())))
    f.queryRepo.offeringQuery(QueryId).value should matchPattern {
      case OfferingQuery(QueryId, OfferingQueryName, _, None, _, _, _, _, _, ChangedLicenseOption, DefaultPriceOption, _) =>
    }
  }

  it should "allow changing OfferingQuery price" in { f =>
    f.mutationRepos.foreach(_.offeringQueryPriceChanged(OfferingQueryPriceChanged(QueryId, ChangedPriceOption, Meta())))
    f.queryRepo.offeringQuery(QueryId).value should matchPattern {
      case OfferingQuery(QueryId, OfferingQueryName, _, None, _, _, _, _, _, DefaultLicenseOption, ChangedPriceOption, _) =>
    }
  }

}
