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

import exchange.api.organization.OrganizationId
import exchange.api.provider._

object Provider extends Types[Provider] {
  type Id = ProviderId
  type Command = ProviderCommand
  type Event = ProviderEvent

  val tag = Tags.aggregateTag("Provider")


  def create = {
    actions
      .rejectCommand {
        case cmd: ProviderCommand if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case CreateProvider(id, organizationId, name, meta) =>
            ProviderCreated(id, organizationId, name, generateSecret, meta)
        }
      }
      .eventHandler {
        case ProviderCreated(id, organizationId, name, _, _) =>
          ActiveProvider(id, name, organizationId)
      }
  }

  def behavior(id: ProviderId) =
    Behavior
      .first {
        create
      }
      .andThen {
        case provider: Provider => provider.acceptCommands
      }
}

sealed trait Provider extends Aggregate[Provider, Provider.Command, Provider.Event]

case class ActiveProvider(id: ProviderId, name: String, organizationId: OrganizationId) extends Provider {
  def acceptCommands =
    Provider.actions
      .rejectCommand {
        case cmd: ProviderCommand if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)
      }
      .commandHandler {
        OneEvent {
          case CreateProvider(_, _, newName, meta) if newName == name =>
            ProviderUnchanged(id, meta)
          case CreateProvider(_, _, newName, meta) =>
            ProviderNameChanged(id, newName, meta)

          case DeleteProvider(_, meta) =>
            ProviderDeleted(id, organizationId, meta)

          case ChangeProviderName(_, newName, meta) if newName == name =>
            ProviderNameUnchanged(id, meta)
          case ChangeProviderName(_, newName, meta) =>
            ProviderNameChanged(id, newName, meta)

          case AddOffering(_, offeringName, localId, rdfUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
            extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, active, meta) =>
            OfferingAdded(id, offeringName, localId, rdfUri, rdfContext, accessWhiteList, endpoints, outputs, inputs,
              extension1, extension2, extension3, spatialExtent, temporalExtent, license, price, active, meta.delayed())
        }
      }
      .eventHandler {
        case ProviderDeleted(_, _, _) =>
          DeletedProvider()
        case ProviderNameChanged(_, newName, _) =>
          copy(name = newName)
        case _ =>
          this
      }
}

case class DeletedProvider() extends Provider {
  def acceptCommands = Provider.create
}
