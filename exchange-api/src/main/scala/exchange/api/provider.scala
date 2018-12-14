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

import monocle.macros.Lenses
import sangria.macros.derive.GraphQLDescription
import microservice._
import microservice.entity.{Entity, Id}

import exchange.api.access.EndpointInput
import exchange.api.license.License
import exchange.api.offering._
import exchange.api.organization.{Organization, OrganizationId}
import exchange.api.price.Price
import exchange.api.semantics.{DataFieldInput, RdfContext}
import exchange.api.extent.{SpatialExtent, TemporalExtent}

object provider {

  val serviceName = "Provider"

  case class ProviderId(value: String) extends OrganizationChildId

  object ProviderId {
    implicit def fromString(aggregateId: String): ProviderId = ProviderId(aggregateId)
  }

  @GraphQLDescription("Offering Provider (Platform or Service)")
  @Lenses
  case class Provider(id: ProviderId, name: String, organization: Option[Organization] = None, secret: String,
                      offerings: List[Offering] = Nil) extends Entity {
    override def show(indent: String) = (s"${indent}Provider($id, $name):" +: offerings.map(_.show(indent+spaces))).mkString("\n")
  }

  sealed trait ProviderCommand extends Command
  sealed trait ProviderEvent extends Event

  case class CreateProvider(id: ProviderId, organizationId: OrganizationId, name: String, meta: Meta = Meta()) extends ProviderCommand
  case class ProviderCreated(id: ProviderId, organizationId: OrganizationId, name: String, secret: String, meta: Meta) extends ProviderEvent
  case class ProviderUnchanged(id: ProviderId, meta: Meta) extends ProviderEvent with Unchanged

  @GraphQLDescription("Delete Provider")
  case class DeleteProvider(id: ProviderId, meta: Meta = Meta()) extends ProviderCommand
  case class ProviderDeleted(id: ProviderId, organizationId: OrganizationId, meta: Meta = Meta()) extends ProviderEvent

  @GraphQLDescription("Change Provider name")
  case class ChangeProviderName(id: ProviderId, name: String, meta: Meta = Meta()) extends ProviderCommand
  case class ProviderNameChanged(id: ProviderId, name: String, meta: Meta) extends ProviderEvent
  case class ProviderNameUnchanged(id: ProviderId, meta: Meta) extends ProviderEvent with Unchanged

  @GraphQLDescription("Add new Offering")
  case class AddOffering(id: ProviderId, name: String, localId: Option[Id], rdfUri: String,
                         rdfContext: Option[RdfContext], accessWhiteList: List[Id] = Nil, endpoints: List[EndpointInput] = Nil,
                         outputs: List[DataFieldInput] = Nil, inputs: List[DataFieldInput] = Nil,
                         extension1: Option[String] = None, extension2: Option[String] = None, extension3: Option[String] = None,
                         spatialExtent: Option[SpatialExtent] = None, temporalExtent: Option[TemporalExtent] = None,
                         license: License, price: Price, activation: Activation = Inactive, meta: Meta = Meta()) extends ProviderCommand
  case class OfferingAdded(id: ProviderId, name: String, localId: Option[Id], rdfUri: String,
                           rdfContext: Option[RdfContext], accessWhiteList: List[Id] = Nil, endpoints: List[EndpointInput] = Nil,
                           outputs: List[DataFieldInput] = Nil, inputs: List[DataFieldInput] = Nil,
                           extension1: Option[String] = None, extension2: Option[String] = None, extension3: Option[String] = None,
                           spatialExtent: Option[SpatialExtent] = None,  temporalExtent: Option[TemporalExtent] = None,
                           license: License, price: Price, activation: Activation, meta: Meta) extends ProviderEvent

  // Errors
  case class ProviderDoesNotExist(override val id: Id, override val meta: Meta) extends Error(id, "ProviderDoesNotExist", meta = meta)

}
