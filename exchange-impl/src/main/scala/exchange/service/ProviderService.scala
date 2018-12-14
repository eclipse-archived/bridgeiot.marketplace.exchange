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

import io.circe.generic.auto._
import microservice._
import microservice.entity._

import exchange.api.organization.{OrganizationEvent, ProviderAdded}
import exchange.api.provider.{AddOffering, CreateProvider, ProviderCommand, ProviderId}
import exchange.api.semantics.OfferingCategoryDoesNotExist
import exchange.api.{organization, provider}
import exchange.model.Provider
import exchange.repo.ExchangeSemanticRepo
import exchange.server.Exchange.{materializer, system}

case class ProviderService(semanticRepo: ExchangeSemanticRepo) extends AkkaServiceBackend(provider.serviceName, Provider.behavior) {
  override def validator = Some({

    case cmd: AddOffering if semanticRepo.offeringCategory(cmd.rdfUri).isEmpty =>
      Left(OfferingCategoryDoesNotExist(cmd.rdfUri, cmd.meta))
  })

  override def eventAdapter = Some(EventAdapterStage(provider.serviceName,
    List(eventTopic[OrganizationEvent](organization.serviceName)), cmdTopic[ProviderCommand](provider.serviceName), {

      case ProviderAdded(organizationId, name, localId, meta) =>
        val id = ProviderId(createId(name, organizationId.value, localId))
        CreateProvider(id, organizationId, name, meta)
    }))
}


