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

import exchange.ExchangeRepoSpec
import exchange.api.semantics._

trait EmptySemanticRepoSpec extends ExchangeRepoSpec {

  def initRepos(repo: Seq[ExchangeRepoMutations]) = {}

  "SemanticRepo" should "contain at least one category" in { f =>
    f.semanticRepo.allOfferingCategories should matchPattern {
      case OfferingCategory(RdfAnnotation(RootOfferingCategoryUri, _, _, _), _, _, _) =>
    }
  }

  it should "contain the root category allOfferings" in { f =>
    f.semanticRepo.offeringCategory(RootOfferingCategoryUri) should matchPattern {
      case OfferingCategory(RdfAnnotation(RootOfferingCategoryUri, _, _, _), _, _, _) =>
    }
  }

  it should "contain at least one data type annotation" in { f =>
      f.semanticRepo.allDataTypes.length should be > 0
  }

}
