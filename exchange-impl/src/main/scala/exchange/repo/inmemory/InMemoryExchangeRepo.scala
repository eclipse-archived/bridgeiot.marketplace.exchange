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
package exchange.repo.inmemory

import exchange.repo.ExchangeSemanticRepo

trait InMemoryExchangeRepo extends InMemoryExchangeRepoQueries with InMemoryExchangeRepoMutations {
  // Empty Exchange
  protected var exchange = Exchange(Nil)
}

class InMemoryExchangeRepoWithoutSemantics(protected val semanticRepo: ExchangeSemanticRepo) extends InMemoryExchangeRepo

case class InMemoryExchangeRepoWithSemantics() extends InMemoryExchangeRepo with InMemoryExchangeSemanticRepo {
  protected val semanticRepo = this
  protected val queryRepo = this
}
