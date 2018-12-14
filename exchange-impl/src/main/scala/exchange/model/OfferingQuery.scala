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
package exchange.model

import io.funcqrs.Tags
import io.funcqrs.behavior.handlers.just.{ManyEvents, OneEvent}
import io.funcqrs.behavior.{Behavior, Types}
import microservice._
import microservice.entity._

import exchange.api.consumer.ConsumerId
import exchange.api.extent.{SpatialExtent, TemporalExtent}
import exchange.api.license.License
import exchange.api.offeringquery._
import exchange.api.price.Price
import exchange.api.semantics.DataField

object OfferingQuery extends Types[OfferingQuery] {
  type Id = OfferingQueryId
  type Command = OfferingQueryCommand
  type Event = OfferingQueryEvent

  val tag = Tags.aggregateTag("OfferingQuery")

  def create = {
    actions
      .rejectCommand {
        case cmd: OfferingQueryCommand if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case CreateOfferingQueryValidated(id, consumerId, name, rdfUri, rdfContext, outputs, inputs, spatialExtent,
                                            temporalExtent, license, price, meta) =>
            OfferingQueryCreated(id, consumerId, name, rdfUri, rdfContext, outputs, inputs, spatialExtent,
                                 temporalExtent, license, price, meta)
        }
      }
      .eventHandler {
        case OfferingQueryCreated(id, consumerId, name, rdfUri, rdfContext, outputs, inputs, spatialExtent,
                                  temporalExtent, license, price, meta) =>
          ActiveOfferingQuery(consumerId, name, rdfUri, outputs, inputs, spatialExtent, temporalExtent, license, price)
      }
  }

  def behavior(id: OfferingQueryId) =
    Behavior
      .first {create}
      .andThen {
        case query: OfferingQuery => query.acceptCommands
      }
}

sealed trait OfferingQuery extends Aggregate[OfferingQuery, OfferingQuery.Command, OfferingQuery.Event]

case class ActiveOfferingQuery(consumerId: ConsumerId, name: String, categoryUri: Option[String],
                               outputs: List[DataField] = Nil, inputs: List[DataField] = Nil,
                               spatialExtent: Option[SpatialExtent], temporalExtent: Option[TemporalExtent],
                               license: Option[License], price: Option[Price],
                               subscriptions: Set[Id] = Set.empty) extends OfferingQuery {
  def acceptCommands =
    OfferingQuery.actions
      .rejectCommand {
        case cmd: OfferingQueryCommand if !cmd.isInstanceOf[UnsubscribeQueryFromOffering] && cmd.hasWrongOrganization =>
          NotAuthorized(cmd)

        case cmd@UnsubscribeQueryFromOffering(queryId, offeringId, meta) if meta.hasWrongOrganization(queryId) && meta.hasWrongOrganization(offeringId) =>
          NotAuthorized(cmd)
        case UnsubscribeQueryFromOffering(id, offeringId, meta) if !(subscriptions contains offeringId) =>
          QueryNotSubscribedToOffering(id, offeringId, meta)
      }
      .commandHandler {
        OneEvent {
          case DeleteOfferingQuery(id, meta) =>
            OfferingQueryDeleted(id, consumerId, meta)

          case ChangeOfferingQueryName(id, newName, meta) if newName == name =>
            OfferingQueryNameUnchanged(id, meta)
          case ChangeOfferingQueryName(id, newName, meta) =>
            OfferingQueryNameChanged(id, newName, meta)

          case ChangeOfferingQueryCategory(id, newCategoryUri, meta) if newCategoryUri == categoryUri =>
            OfferingQueryCategoryUnchanged(id, meta)

          case ChangeOfferingQueryInputsValidated(id, newInputs, meta) if newInputs == inputs =>
            OfferingQueryInputsUnchanged(id, meta)
          case ChangeOfferingQueryInputsValidated(id, newInputData, meta) =>
            OfferingQueryInputsChanged(id, newInputData, meta)

          case ChangeOfferingQueryOutputsValidated(id, newOutputs, meta) if newOutputs == outputs =>
            OfferingQueryOutputsUnchanged(id, meta)
          case ChangeOfferingQueryOutputsValidated(id, newOutputData, meta) =>
            OfferingQueryOutputsChanged(id, newOutputData, meta)

          case ChangeOfferingQuerySpatialExtent(id, newSpatialExtent, meta) if newSpatialExtent == spatialExtent =>
            OfferingQuerySpatialExtentUnchanged(id, meta)
          case ChangeOfferingQuerySpatialExtent(id, newSpatialExtent, meta) =>
            OfferingQuerySpatialExtentChanged(id, newSpatialExtent, meta)

          case ChangeOfferingQueryTemporalExtent(id, newTemporalExtent, meta) if newTemporalExtent == temporalExtent =>
            OfferingQueryTemporalExtentUnchanged(id, meta)
          case ChangeOfferingQueryTemporalExtent(id, newTemporalExtent, meta) =>
            OfferingQueryTemporalExtentChanged(id, newTemporalExtent, meta)

          case ChangeOfferingQueryLicense(id, newLicense, meta) if newLicense == license =>
            OfferingQueryLicenseUnchanged(id, meta)
          case ChangeOfferingQueryLicense(id, newLicense, meta) =>
            OfferingQueryLicenseChanged(id, newLicense, meta)

          case ChangeOfferingQueryPrice(id, newPrice, meta) if newPrice == price =>
            OfferingQueryPriceUnchanged(id, meta)
          case ChangeOfferingQueryPrice(id, newPrice, meta) =>
            OfferingQueryPriceChanged(id, newPrice, meta)

          case SubscribeQueryToOffering(id, offeringId, meta) =>
            SubscribedQueryToOffering(id, offeringId, meta.delayed())
          case UnsubscribeQueryFromOffering(id, offeringId, meta) =>
            UnsubscribedQueryFromOffering(id, offeringId, meta.delayed())
        }
      }
      .commandHandler {
        ManyEvents {
          case CreateOfferingQueryValidated(id, _, newName, newRdfUri, _, newOutputs, newInputs, newSpatialExtent,
                                   newTemporalExtent, newLicense, newPrice, meta) =>
            var events: List[OfferingQueryEvent] = Nil
            if (newName != name) events ::= OfferingQueryNameChanged(id, name, meta.copy())
            if (newRdfUri != categoryUri) events ::= OfferingQueryCategoryChanged(id, newRdfUri, meta.copy())
            if (newInputs != inputs) events ::= OfferingQueryInputsChanged(id, newInputs, meta.copy())
            if (newOutputs != outputs) events ::= OfferingQueryOutputsChanged(id, newOutputs, meta.copy())
            if (newSpatialExtent != spatialExtent) events ::= OfferingQuerySpatialExtentChanged(id, newSpatialExtent, meta.copy())
            if (newTemporalExtent != temporalExtent) events ::= OfferingQueryTemporalExtentChanged(id, newTemporalExtent, meta.copy())
            if (newLicense != license) events ::= OfferingQueryLicenseChanged(id, newLicense, meta.copy())
            if (newPrice != price) events ::= OfferingQueryPriceChanged(id, newPrice, meta.copy())
            if (events.isEmpty)
              List(OfferingQueryUnchanged(id, meta))
            else
              markLast(events)
          case ChangeOfferingQueryCategory(id, newRdfUri, meta) =>
            var events: List[OfferingQueryEvent] = List(OfferingQueryCategoryChanged(id, newRdfUri, meta))
            if (inputs.nonEmpty) events ::= OfferingQueryInputsChanged(id, Nil, meta.copy())
            if (outputs.nonEmpty) events ::= OfferingQueryOutputsChanged(id, Nil, meta.copy())
            markLast(events)
        }
      }
      .eventHandler {
        case OfferingQueryDeleted(_, _, _) =>
          DeletedOfferingQuery()
        case OfferingQueryNameChanged(_, newName, _) =>
          copy(name = newName)
        case OfferingQueryCategoryChanged(_, newRdfUri, _) =>
          copy(categoryUri = newRdfUri)
        case OfferingQueryInputsChanged(_, newInputData, _) =>
          copy(inputs = newInputData)
        case OfferingQueryOutputsChanged(_, newOutputData, _) =>
          copy(outputs = newOutputData)
        case OfferingQuerySpatialExtentChanged(_, newSpatialExtent, _) =>
          copy(spatialExtent = newSpatialExtent)
        case OfferingQueryTemporalExtentChanged(_, newTemporalExtent, _) =>
          copy(temporalExtent = newTemporalExtent)
        case OfferingQueryLicenseChanged(_, newLicense, _) =>
          copy(license = newLicense)
        case OfferingQueryPriceChanged(_, newPrice, _) =>
          copy(price = newPrice)
        case SubscribedQueryToOffering(_, offeringId, _) =>
          copy(subscriptions = subscriptions + offeringId)
        case UnsubscribedQueryFromOffering(_, offeringId, _) =>
          copy(subscriptions = subscriptions - offeringId)
        case _ =>
          this
      }
}

case class DeletedOfferingQuery() extends OfferingQuery {
  def acceptCommands = OfferingQuery.create
}
