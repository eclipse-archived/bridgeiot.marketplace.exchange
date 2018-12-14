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

import exchange.api.offering._
import exchange.api.provider.{OfferingAdded, ProviderDeleted, ProviderEvent}
import exchange.api.semantics.{InvalidDataTypes, OfferingCategoryDoesNotExist}
import exchange.api.{offering, provider}
import exchange.model.Offering
import exchange.repo.{ExchangeRepoQueries, ExchangeSemanticRepo}
import exchange.server.Exchange.{materializer, system}

case class OfferingService(queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo) extends
  AkkaServiceBackend(offering.serviceName, Offering.behavior) with DataTypeValidator {

  override def validator = Some({

    case CreateOffering(id, providerId, name, categoryUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
    extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, active, meta) =>
      val invalidTypes = invalidDataTypes(categoryUri, "input", true, inputs) ++
        invalidDataTypes(categoryUri, "output", true, outputs)
      if (semanticRepo.offeringCategory(categoryUri).isEmpty)
        Left(OfferingCategoryDoesNotExist(categoryUri, meta))
      else if (invalidTypes.isEmpty)
        Right(CreateOfferingValidated(id, providerId, name, categoryUri, rdfContext, accessWhiteList,
          endpoints map (_.toEndpoint), outputs flatMap toDataField(categoryUri), inputs flatMap toDataField(categoryUri),
          extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, active, meta))
      else
        Left(InvalidDataTypes(id, invalidTypes, meta))

    case ChangeOfferingInputs(id, newInputs, meta) =>
      queryRepo.offering(id).map { offering =>
        val categoryUri = offering.rdfAnnotation.uri
        val invalidTypes = invalidDataTypes(categoryUri, "input", true, newInputs)
        if (invalidTypes.isEmpty)
          Right(ChangeOfferingInputsValidated(id, newInputs flatMap toDataField(categoryUri), meta))
        else
          Left(InvalidDataTypes(id, invalidTypes, meta))
      }.getOrElse(Left(OfferingDoesNotExist(id, meta)))

    case ChangeOfferingOutputs(id, newOutputs, meta) =>
      queryRepo.offering(id).map { offering =>
        val categoryUri = offering.rdfAnnotation.uri
        val invalidTypes = invalidDataTypes(categoryUri, "output", true, newOutputs)
        if (invalidTypes.isEmpty)
          Right(ChangeOfferingOutputsValidated(id, newOutputs flatMap toDataField(categoryUri), meta))
        else
          Left(InvalidDataTypes(id, invalidTypes, meta))
      }.getOrElse(Left(OfferingDoesNotExist(id, meta)))

    case ChangeOfferingCategory(id, categoryUri, meta) if semanticRepo.offeringCategory(categoryUri).isEmpty =>
      Left(OfferingCategoryDoesNotExist(categoryUri, meta))
  })

  override def eventAdapter = Some(EventAdapterStage(offering.serviceName,
    List(eventTopic[ProviderEvent](provider.serviceName)), cmdTopic[OfferingCommand](offering.serviceName), {

      case OfferingAdded(organizationId, name, localId, categoryUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
      extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, active, meta) =>
        val id = OfferingId(createId(name, organizationId.value, localId))
        CreateOffering(id, organizationId, name, categoryUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
          extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, active, meta)

      case ProviderDeleted(providerId, _, meta) => queryRepo.provider(providerId) map { provider =>
        provider.offerings map { offering =>
          DeleteOffering(offering.id, meta)
        }
      } getOrElse Nil
    }))
}
