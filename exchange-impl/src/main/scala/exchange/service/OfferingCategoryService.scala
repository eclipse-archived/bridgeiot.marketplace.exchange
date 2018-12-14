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
package exchange.service

import microservice._

import exchange.api.semantics
import exchange.api.semantics._
import exchange.model.OfferingCategory
import exchange.repo.ExchangeSemanticRepo
import exchange.server.Exchange.system

case class OfferingCategoryService(semanticRepo: ExchangeSemanticRepo) extends AkkaServiceBackend(semantics.serviceName, OfferingCategory.behavior) {
  override def validator = Some({

    case cmd: CreateOfferingCategory if semanticRepo.offeringCategory(cmd.parent).isEmpty =>
      Left(OfferingCategoryParentDoesNotExist(cmd.id, cmd.parent, cmd.meta))

    case cmd: ChangeOfferingCategoryCommand if cmd.uri.isEmpty =>
      Left(EmptyNameOfferingCategory("", cmd.meta))

    case cmd: ChangeOfferingCategoryCommand if semanticRepo.offeringCategory(cmd.uri).isEmpty =>
      Left(OfferingCategoryDoesNotExist(cmd.uri, cmd.meta))

    case cmd: AddTypeToOfferingCategoryCommand =>
      Right(cmd)

    case cmd: ChangeTypeForOfferingCategoryCommand =>
      Right(cmd)

    case cmd: ChangeOfferingCategoryCommand if !semanticRepo.offeringCategory(cmd.uri).exists(_.rdfAnnotation.proposed) =>
      Left(NotAuthorized(cmd, "only proposed categories are allowed to be changed"))

  })
}
