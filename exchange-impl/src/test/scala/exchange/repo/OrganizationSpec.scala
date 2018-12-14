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
import exchange.api.organization._

trait OrganizationSpec extends ExchangeRepoSpec {

  def initRepos(repos: Seq[ExchangeRepoMutations]) = {
    repos.foreach(_.organizationCreated(OrganizationCreated(OrgId, OrgName, Meta())))
    repos.foreach(_.organizationCreated(OrganizationCreated(OtherOrgId, OtherOrgName, Meta())))
  }

  "ExchangeRepo" should "contain Organization after creation" in { f =>
    f.queryRepo.organization(OrgId).value shouldBe Organization(OrgId, OrgName, Nil, Nil)
  }

  it should "contain all Organizations after creation" in { f =>
    f.queryRepo.allOrganizations.length shouldBe 2
  }

  it should "allow changing Organization name" in { f =>
    f.mutationRepos.foreach(_.organizationNameChanged(OrganizationNameChanged(OrgId, ChangedOrgName, Meta())))
    f.queryRepo.organization(OrgId).value shouldBe Organization(OrgId, ChangedOrgName, Nil, Nil)
  }

}
