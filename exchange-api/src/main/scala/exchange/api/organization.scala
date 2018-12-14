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
package exchange.api

import scala.language.implicitConversions

import io.funcqrs.AggregateId
import microservice._
import microservice.entity.{Entity, Id, createId}
import monocle.macros.Lenses
import sangria.macros.derive.GraphQLDescription

import exchange.api.consumer._
import exchange.api.provider._

object organization {

  val serviceName = "Organization"

  case class OrganizationId(value: String) extends AggregateId

  object OrganizationId {
    implicit def fromString(aggregateId: String): OrganizationId = OrganizationId(aggregateId)
  }

  @GraphQLDescription("Organization of Offering Provider / Consumer")
  @Lenses
  case class Organization(id: OrganizationId, name: String, providers: List[Provider] = Nil, consumers: List[Consumer] = Nil) extends Entity {
    override def show(indent: String) = (s"${indent}Organization($id, $name):" +:
      providers.map(_.show(indent+spaces)) ++: consumers.map(_.show(indent+spaces))).mkString("\n")
  }

  sealed trait OrganizationCommand extends Command
  sealed trait OrganizationEvent extends Event

  @GraphQLDescription("Create new Organization")
  case class CreateOrganization(name: String, localId: Option[Id], meta: Meta = Meta()) extends OrganizationCommand with
    HierarchicalCreationCommand {
      val parentId = ""
      def id = OrganizationId(createId(name, parentId, localId))
    }
  case class OrganizationCreated(id: OrganizationId, name: String, meta: Meta) extends OrganizationEvent

  @GraphQLDescription("Change Organization name")
  case class ChangeOrganizationName(id: OrganizationId, name: String, meta: Meta = Meta()) extends OrganizationCommand
  case class OrganizationNameChanged(id: OrganizationId, name: String, meta: Meta) extends OrganizationEvent
  case class OrganizationNameUnchanged(id: OrganizationId, meta: Meta) extends OrganizationEvent with Unchanged

  @GraphQLDescription("Add new OfferingProvider")
  case class AddProvider(id: OrganizationId, name: String, localId: Option[Id], meta: Meta = Meta()) extends OrganizationCommand
  case class ProviderAdded(id: OrganizationId, name: String, localId: Option[Id], meta: Meta) extends OrganizationEvent

  @GraphQLDescription("Add new OfferingConsumer")
  case class AddConsumer(id: OrganizationId, name: String, localId: Option[Id], meta: Meta = Meta()) extends OrganizationCommand
  case class ConsumerAdded(id: OrganizationId, name: String, localId: Option[Id], meta: Meta) extends OrganizationEvent

  // Errors
  case class OrganizationAlreadyExists(override val id: Id, override val meta: Meta) extends Error(id, "OrganizationExists", meta = meta)
  case class OrganizationDoesNotExist(override val id: Id, override val meta: Meta) extends Error(id, "OrganizationDoesNotExist", meta = meta)

  // Wrapper
  @GraphQLDescription("List of Organizations")
  case class Organizations(organizations: List[Organization])

}
