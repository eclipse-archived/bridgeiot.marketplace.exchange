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
import microservice.entity.createId

import exchange.api.consumer._
import exchange.api.offering.OfferingDoesNotExist
import exchange.api.organization.{ConsumerAdded, OrganizationEvent}
import exchange.api.semantics.OfferingCategoryDoesNotExist
import exchange.api.{consumer, organization}
import exchange.model.Consumer
import exchange.repo.{ExchangeRepoQueries, ExchangeSemanticRepo}
import exchange.server.Exchange.{materializer, system}

case class ConsumerService(queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo) extends
  AkkaServiceBackend(consumer.serviceName, Consumer.behavior) {

  override def validator = Some({
    case cmd: AddOfferingQuery if cmd.rdfUri.isDefined && semanticRepo.offeringCategory(cmd.rdfUri.get).isEmpty =>
      Left(OfferingCategoryDoesNotExist(cmd.rdfUri.get, cmd.meta))

    case cmd@SubscribeConsumerToOffering(_, offeringId, meta) =>
      if (queryRepo.offering(offeringId).isEmpty) Left(OfferingDoesNotExist(offeringId, meta))
      else Right(cmd)
  })

  override def eventAdapter = Some(EventAdapterStage(consumer.serviceName,
    List(eventTopic[OrganizationEvent](organization.serviceName)), cmdTopic[ConsumerCommand](consumer.serviceName), {

      case ConsumerAdded(organizationId, name, localId, meta) =>
        val id = ConsumerId(createId(name, organizationId.value, localId))
        CreateConsumer(id, organizationId, name, meta)
   }))
}

