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

import exchange.api.organization._

object Organization extends Types[Organization] {
  type Id = OrganizationId
  type Command = OrganizationCommand
  type Event = OrganizationEvent

  val tag = Tags.aggregateTag("Organization")

  def create = {
    actions
      .rejectCommand {
        case cmd: OrganizationCommand if cmd.isAnonymous  =>
          NotAuthorized(cmd)
        case cmd: CreateOrganization if cmd.hasOrganization =>
          NotAuthorized(cmd, s"already assigned to Organization ${cmd.meta.requesterOrgId.get}")
      }
      .commandHandler {
        OneEvent {
          case CreateOrganization(name, localId, meta) =>
            OrganizationCreated(createId(name, "", localId), name, meta)
        }
      }
      .eventHandler {
        case OrganizationCreated(id, name, _) =>
          ActiveOrganization(id, name)
      }
  }

  def behavior(id: OrganizationId): Behavior[Organization, Command, Event] =
    Behavior
      .first {
        create
      }
      .andThen {
        case organization: ActiveOrganization => organization.acceptCommands
      }
}

sealed trait Organization {
  def id: OrganizationId
}

case class ActiveOrganization(id: OrganizationId, name: String) extends Organization {
  def acceptCommands =
    Organization.actions
      .rejectCommand {
        case cmd: OrganizationCommand if cmd.hasWrongOrganization =>
          NotAuthorized(cmd)

        case CreateOrganization(_, _, meta) =>
          OrganizationAlreadyExists(id, meta)
      }
      .commandHandler {
        OneEvent {
          case ChangeOrganizationName(_, newName, meta) if newName == name =>
            OrganizationNameUnchanged(id, meta)
          case ChangeOrganizationName(_, newName, meta) =>
            OrganizationNameChanged(id, newName, meta)

          case AddProvider(_, providerName, localId, meta) =>
            ProviderAdded(id, providerName, localId, meta.delayed())
          case AddConsumer(_, consumerName, localId, meta) =>
            ConsumerAdded(id, consumerName, localId, meta.delayed())
        }
      }
      .eventHandler {
        case OrganizationNameChanged(_, newName, _) =>
          copy(name = newName)
        case _ =>
          this
      }
}
