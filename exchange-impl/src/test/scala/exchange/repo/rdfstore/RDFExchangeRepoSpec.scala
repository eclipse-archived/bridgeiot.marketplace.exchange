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
package exchange.repo.rdfstore

import exchange.ExchangeRepoSpec
import exchange.repo.ExchangeSemanticRepo
import exchange.repo.inmemory.InMemoryExchangeRepoWithoutSemantics

trait RDFExchangeRepoSpec extends ExchangeRepoSpec {

  def createRepos = {
    val rdfRepo = new RDFExchangeRepo
    val inMemoryRepo = new InMemoryExchangeRepoWithoutSemantics(rdfRepo)
    (inMemoryRepo, rdfRepo, List(inMemoryRepo, rdfRepo))
  }

  def destroySemanticRepo(repo: ExchangeSemanticRepo) = {
     RDFServer.get().executeUpdateQuery(QueryFactory.deleteGraph(QueryFactory.getOfferingGraph))
  }

}
