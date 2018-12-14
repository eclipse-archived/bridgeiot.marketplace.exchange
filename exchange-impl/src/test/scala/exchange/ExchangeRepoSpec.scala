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
package exchange

import org.scalatest.{Matchers, OptionValues, fixture}

import exchange.repo.{ExchangeSemanticRepo, ExchangeRepoMutations, ExchangeRepoQueries}


trait ExchangeRepoSpec extends fixture.FlatSpec with Matchers with OptionValues with ExchangeSpec {

  case class FixtureParam(queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo, mutationRepos: Seq[ExchangeRepoMutations])

  def withFixture(test: OneArgTest) = {
    val (queryRepo, semanticRepo, mutationRepos) = createRepos
    initRepos(mutationRepos)
    val res = test(FixtureParam(queryRepo, semanticRepo, mutationRepos))
    destroySemanticRepo(semanticRepo)
    res
  }

  def createRepos: (ExchangeRepoQueries, ExchangeSemanticRepo, Seq[ExchangeRepoMutations])

  def initRepos(repo: Seq[ExchangeRepoMutations]): Unit

  def destroySemanticRepo(repo: ExchangeSemanticRepo): Unit

}
