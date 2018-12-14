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
import exchange.api.offering._
import exchange.api.organization.OrganizationCreated
import exchange.api.provider.ProviderCreated
import exchange.api.semantics.RdfAnnotation

trait OfferingSpec extends ExchangeRepoSpec {

  def initRepos(repos: Seq[ExchangeRepoMutations]) = {
    repos.foreach(_.organizationCreated(OrganizationCreated(OrgId, OrgName, Meta())))
    repos.foreach(_.providerCreated(ProviderCreated(ProvId, OrgId, ProviderName, Secret, Meta())))
    repos.foreach(_.offeringCreated(OfferingCreated(OffId, ProvId, OfferingName, CategoryUri, None, NoOfferingAccessWhiteList,
      OfferingEndpoints, OutputDataFields, InputDataFields, None, None, None, SpatialExtent, TemporalExtent, DefaultLicense, DefaultPrice, Inactive, Meta())))
    repos.foreach(_.offeringCreated(OfferingCreated(OtherOfferingId, ProvId, OtherOfferingName, OtherCategoryUri, None, NoOfferingAccessWhiteList,
      OfferingEndpoints, OutputDataFields, InputDataFields, None, None, None, SpatialExtent, TemporalExtent, DefaultLicense, DefaultPrice, Inactive, Meta())))
  }

  "ExchangeRepo" should "contain Offering after creation" in { f =>
    f.queryRepo.offering(OffId).value should matchPattern {
      case Offering(OffId, OfferingName, _, _, _, None, _, _, _, _, _, _, _, _, _, DefaultLicense, DefaultPrice) =>
    }
  }

  it should "contain all Offerings after creation" in { f =>
    f.queryRepo.allOfferings(None, None).length shouldBe 2
  }

  it should "contain all Offerings in the same category after creation" ignore { f =>
    f.queryRepo.allOfferings(Some(CategoryUri), None).length shouldBe 1
  }

  it should "contain all Offerings in the same category for some Organization after creation" ignore { f =>
    f.queryRepo.offeringsForOrganization(Some(CategoryUri), None)(OrgId).length shouldBe 1
  }

  it should "allow changing Offering name" in { f =>
    f.mutationRepos.foreach(_.offeringNameChanged(OfferingNameChanged(OffId, ChangedOfferingName, Meta())))
    f.queryRepo.offering(OffId).value should matchPattern {
      case Offering(OffId, ChangedOfferingName, _, _, _, None, _, _, _, _, _, _, _, _, _, DefaultLicense, DefaultPrice) =>
    }
  }

  it should "allow changing Offering type" in { f =>
    f.mutationRepos.foreach(_.offeringCategoryChanged(OfferingCategoryChanged(OffId, ChangedCategoryUri, Meta())))
    f.queryRepo.offering(OffId).value should matchPattern {
      case Offering(OffId, OfferingName, _, _, RdfAnnotation(ChangedCategoryUri, _, _, _), None, _, _, _, _, _, _, _, _, _, DefaultLicense, DefaultPrice) =>
    }
  }

  // TODO: add tests for changing endpoints, inputs, outputs

  it should "allow changing Offering license" in { f =>
    f.mutationRepos.foreach(_.offeringLicenseChanged(OfferingLicenseChanged(OffId, ChangedLicense, Meta())))
    f.queryRepo.offering(OffId).value should matchPattern {
      case Offering(OffId, OfferingName, _, _, _, None, _, _, _, _, _, _, _, _, _, ChangedLicense, DefaultPrice) =>
    }
  }

  it should "allow changing Offering price" in { f =>
    f.mutationRepos.foreach(_.offeringPriceChanged(OfferingPriceChanged(OffId, ChangedPrice, Meta())))
    f.queryRepo.offering(OffId).value should matchPattern {
      case Offering(OffId, OfferingName, _, _, _, None, _, _, _, _, _, _, _, _, _, DefaultLicense, ChangedPrice) =>
    }
  }

}
