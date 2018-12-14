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
import exchange.api.provider._

trait ProviderSpec extends ExchangeRepoSpec {

  def initRepos(repos: Seq[ExchangeRepoMutations]) = {
    repos.foreach(_.organizationCreated(OrganizationCreated(OrgId, OrgName, Meta())))
    repos.foreach(_.providerCreated(ProviderCreated(ProvId, OrgId, ProviderName, Secret, Meta())))
    repos.foreach(_.providerCreated(ProviderCreated(OtherProviderId, OrgId, OtherProviderName, Secret, Meta())))
  }

  "ExchangeRepo" should "contain Provider after creation" in { f =>
    f.queryRepo.provider(ProvId).value should matchPattern {
      case Provider(ProvId, ProviderName, _, _, _) =>
    }
  }

 it should "contain all Providers for Organization after creation" in { f =>
    f.queryRepo.providersForOrganization()(OrgId).length shouldBe 2
  }

  it should "allow changing Provider name" in { f =>
    f.mutationRepos.foreach(_.providerNameChanged(ProviderNameChanged(ProvId, ChangedProviderName, Meta())))
    f.queryRepo.provider(ProvId).value should matchPattern {
      case Provider(ProvId, ChangedProviderName, _, _, _) =>
    }
  }

}
