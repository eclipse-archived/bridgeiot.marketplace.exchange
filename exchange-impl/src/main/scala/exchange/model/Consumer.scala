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
import io.funcqrs.behavior.handlers.just.OneEvent
import io.funcqrs.behavior.{Behavior, Types}
import microservice._
import microservice.entity._
import microservice.security._

import exchange.api.consumer._
import exchange.api.organization.OrganizationId

object Consumer extends Types[Consumer] {
  type Id = ConsumerId
  type Command = ConsumerCommand
  type Event = ConsumerEvent

  val tag = Tags.aggregateTag("Consumer")

  def create = {
    actions
      .rejectCommand {
        case cmd: ConsumerCommand if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case CreateConsumer(id, organizationId, name, meta) =>
            ConsumerCreated(id, organizationId, name, generateSecret, meta)
        }
      }
      .eventHandler {
        case ConsumerCreated(id, organizationId, name, _, _) =>
          ActiveConsumer(id, name, organizationId)
      }
  }

  def behavior(id: ConsumerId) =
    Behavior
      .first {create}
      .andThen {
        case consumer: Consumer => consumer.acceptCommands
      }
}

sealed trait Consumer extends Aggregate[Consumer, Consumer.Command, Consumer.Event]

case class ActiveConsumer(id: ConsumerId, name: String, organizationId: OrganizationId, subscriptions: Set[Id] = Set.empty) extends Consumer {
  def acceptCommands =
    Consumer.actions
      .rejectCommand {
        case cmd: ConsumerCommand if !cmd.isInstanceOf[UnsubscribeConsumerFromOffering] && cmd.hasWrongOrganization =>
          NotAuthorized(cmd)

        case cmd@UnsubscribeConsumerFromOffering(queryId, offeringId, meta) if meta.hasWrongOrganization(queryId) && meta.hasWrongOrganization(offeringId) =>
          NotAuthorized(cmd)
        case UnsubscribeConsumerFromOffering(_, offeringId, meta) if !(subscriptions contains offeringId) =>
          ConsumerNotSubscribedToOffering(id, offeringId, meta)
      }
      .commandHandler {
        OneEvent {
          case CreateConsumer(_, _, newName, meta) if newName == name =>
            ConsumerUnchanged(id, meta)
          case CreateConsumer(_, _, newName, meta) =>
            ConsumerNameChanged(id, newName, meta)

          case DeleteConsumer(_, meta) =>
            ConsumerDeleted(id, organizationId, meta)
            
          case ChangeConsumerName(_, newName, meta) if newName == name =>
            ConsumerNameUnchanged(id, meta)
          case ChangeConsumerName(_, newName, meta) =>
            ConsumerNameChanged(id, newName, meta)

          case AddOfferingQuery(_, queryName, localId, rdfUri, rdfContext, outputs, inputs, spatialExtent,
                                temporalExtent, license, price, meta) =>
            OfferingQueryAdded(id, queryName, localId, rdfUri, rdfContext, outputs, inputs, spatialExtent,
              temporalExtent, license, price, meta.delayed())

          case SubscribeConsumerToOffering(_, offeringId, meta) =>
            SubscribedConsumerToOffering(id, offeringId, meta.delayed())
          case UnsubscribeConsumerFromOffering(_, offeringId, meta) =>
            UnsubscribedConsumerFromOffering(id, offeringId, meta.delayed())
        }
      }
      .eventHandler {
        case ConsumerDeleted(_, _, _) =>
          DeletedConsumer()

        case ConsumerNameChanged(_, newName, _) =>
          copy(name = newName)

        case SubscribedConsumerToOffering(_, offeringId, _) =>
          copy(subscriptions = subscriptions + offeringId)
        case UnsubscribedConsumerFromOffering(_, offeringId, _) =>
          copy(subscriptions = subscriptions - offeringId)

        case _ =>
          this
      }
}

case class DeletedConsumer() extends Consumer {
  def acceptCommands = Consumer.create
}
