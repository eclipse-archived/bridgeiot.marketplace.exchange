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
import exchange.api.offeringquery._
import exchange.api.semantics.{InvalidDataTypes, NoDataFieldsAllowedWithoutOfferingCategory, OfferingCategoryDoesNotExist}
import exchange.api.{consumer, offeringquery}
import exchange.model.OfferingQuery
import exchange.repo.{ExchangeRepoQueries, ExchangeSemanticRepo}
import exchange.server.Exchange.{materializer, system}

case class OfferingQueryService(queryRepo: ExchangeRepoQueries, semanticRepo: ExchangeSemanticRepo) extends
  AkkaServiceBackend(offeringquery.serviceName, OfferingQuery.behavior) with DataTypeValidator {

  override def validator = Some({

    case CreateOfferingQuery(id, consumerId, name, categoryUriOpt, rdfContext, outputs, inputs,
    spatialExtent, temporalExtent, license, price, meta) =>
      categoryUriOpt.map { categoryUri =>
        val invalidTypes = invalidDataTypes(categoryUri, "input", false, inputs) ++
          invalidDataTypes(categoryUri, "output", false, outputs)
        if (semanticRepo.offeringCategory(categoryUri).isEmpty)
          Left(OfferingCategoryDoesNotExist(categoryUri, meta))
        else if (invalidTypes.isEmpty)
          Right(CreateOfferingQueryValidated(id, consumerId, name, categoryUriOpt, rdfContext,
            outputs flatMap toDataField(categoryUri), inputs flatMap toDataField(categoryUri),
            spatialExtent, temporalExtent, license, price, meta))
        else
          Left(InvalidDataTypes(id, invalidTypes, meta))
      }.getOrElse(if (outputs.isEmpty && inputs.isEmpty)
        Right(CreateOfferingQueryValidated(id, consumerId, name, categoryUriOpt, rdfContext, Nil, Nil, spatialExtent, temporalExtent, license, price, meta))
      else Left(NoDataFieldsAllowedWithoutOfferingCategory(id, meta)))

    case ChangeOfferingQueryInputs(id, newInputs, meta) =>
      queryRepo.offeringQuery(id).map { offeringQuery =>
        offeringQuery.rdfAnnotation.map { rdfAnnotation =>
          val categoryUri = rdfAnnotation.uri
          val invalidTypes = invalidDataTypes(categoryUri, "input", false, newInputs)
          if (invalidTypes.isEmpty)
            Right(ChangeOfferingQueryInputsValidated(id, newInputs flatMap toDataField(categoryUri), meta))
          else
            Left(InvalidDataTypes(id, invalidTypes, meta))
        }.getOrElse(Left(NoDataFieldsAllowedWithoutOfferingCategory(id, meta)))
      }.getOrElse(Left(OfferingQueryDoesNotExist(id, meta)))

    case ChangeOfferingQueryOutputs(id, newOutputs, meta) =>
      queryRepo.offeringQuery(id).map { offeringQuery =>
        offeringQuery.rdfAnnotation.map { rdfAnnotation =>
          val categoryUri = rdfAnnotation.uri
          val invalidTypes = invalidDataTypes(categoryUri, "output", false, newOutputs)
          if (invalidTypes.isEmpty)
            Right(ChangeOfferingQueryOutputsValidated(id, newOutputs flatMap toDataField(categoryUri), meta))
          else
            Left(InvalidDataTypes(id, invalidTypes, meta))
        }.getOrElse(Left(NoDataFieldsAllowedWithoutOfferingCategory(id, meta)))
      }.getOrElse(Left(OfferingQueryDoesNotExist(id, meta)))
    case cmd@ChangeOfferingQueryCategory(id, Some(rdfUri), meta) if semanticRepo.offeringCategory(rdfUri).isEmpty =>
      Left(OfferingCategoryDoesNotExist(rdfUri, meta))

    case cmd@SubscribeQueryToOffering(_, offeringId, meta) =>
      if (queryRepo.offering(offeringId).isEmpty) Left(OfferingDoesNotExist(offeringId, meta))
      else Right(cmd)
  })

  override def eventAdapter = Some(EventAdapterStage(offeringquery.serviceName,
    List(eventTopic[ConsumerEvent](consumer.serviceName)), cmdTopic[OfferingQueryCommand](offeringquery.serviceName), {

      case OfferingQueryAdded(organizationId, name, localId, rdfContext, categoryUri, outputs, inputs, spatialExtent, temporalExtent, license, price, meta) =>
        val id = OfferingQueryId(createId(name, organizationId.value, localId))
        CreateOfferingQuery(id, organizationId, name, categoryUri, rdfContext, outputs, inputs, spatialExtent,
          temporalExtent, license, price, meta)

      case ConsumerDeleted(consumerId, _, meta) => queryRepo.consumer(consumerId) map { consumer =>
        consumer.queries map { query =>
          DeleteOfferingQuery(query.id, meta)
        }
      } getOrElse Nil
    }))
}