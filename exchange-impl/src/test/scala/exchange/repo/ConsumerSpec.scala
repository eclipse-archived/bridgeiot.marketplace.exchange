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
import exchange.api.consumer._

trait ConsumerSpec extends ExchangeRepoSpec {

  def initRepos(repos: Seq[ExchangeRepoMutations]) = {
    repos.foreach(_.organizationCreated(OrganizationCreated(OrgId, OrgName, Meta())))
    repos.foreach(_.consumerCreated(ConsumerCreated(ConsId, OrgId, ConsumerName, Secret, Meta())))
    repos.foreach(_.consumerCreated(ConsumerCreated(OtherConsId, OrgId, OtherConsumerName, Secret, Meta())))
  }

  "ExchangeRepo" should "contain Consumer after creation" in { f =>
    f.queryRepo.consumer(ConsId).value should matchPattern {
      case Consumer(ConsId, ConsumerName, _, _, _) =>
    }
  }

  it should "contain all Consumers for Organization after creation" in { f =>
    f.queryRepo.consumersForOrganization(OrgId).length shouldBe 2
  }

  it should "allow changing Consumer name" in { f =>
    f.mutationRepos.foreach(_.consumerNameChanged(ConsumerNameChanged(ConsId, ChangedConsumerName, Meta())))
    f.queryRepo.consumer(ConsId).value should matchPattern {
      case Consumer(ConsId, ChangedConsumerName, _, _, _) =>
    }
  }

}
